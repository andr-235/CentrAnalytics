import type { FastifyInstance, FastifyReply } from "fastify";
import { z } from "zod";

import { TelegramAuthError, type ConfirmSessionResponse, type StartSessionResponse } from "./telegram-auth.types.js";
import type { TelegramAuthService } from "./telegram-auth.service.js";

const startSessionSchema = z.object({
  phoneNumber: z.string().min(1)
});

const confirmSessionSchema = z.object({
  transactionId: z.string().min(1),
  code: z.string().min(1),
  password: z.string().min(1).nullish()
});

export function registerTelegramAuthRoutes(
  app: FastifyInstance,
  telegramAuthService: TelegramAuthService
): void {
  app.get("/session/current", async (_request, reply) => {
    const current = await telegramAuthService.getCurrentSession();
    if (!current) {
      return reply.send(null);
    }

    return reply.send(current);
  });

  app.post<{ Reply: StartSessionResponse | { error: string } }>("/session/start", async (request, reply) => {
    try {
      const payload = startSessionSchema.parse(request.body);
      return await telegramAuthService.startSession(payload.phoneNumber);
    } catch (error) {
      return sendTelegramAuthError(reply, error);
    }
  });

  app.post<{ Reply: ConfirmSessionResponse | { error: string } }>("/session/confirm", async (request, reply) => {
    try {
      const payload = confirmSessionSchema.parse(request.body);
      return await telegramAuthService.confirmSession(
        payload.transactionId,
        payload.code,
        payload.password ?? undefined
      );
    } catch (error) {
      return sendTelegramAuthError(reply, error);
    }
  });

  app.delete("/session/current", async (_request, reply) => {
    try {
      await telegramAuthService.resetCurrentSession();
      return reply.status(204).send();
    } catch (error) {
      return sendTelegramAuthError(reply, error);
    }
  });
}

function sendTelegramAuthError(reply: FastifyReply, error: unknown) {
  if (error instanceof z.ZodError) {
    return reply.status(400).send({
      error: "INVALID_REQUEST"
    });
  }

  if (error instanceof TelegramAuthError) {
    return reply.status(400).send({
      error: error.code
    });
  }

  throw error;
}
