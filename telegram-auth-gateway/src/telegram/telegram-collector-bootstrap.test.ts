import { describe, expect, it, vi } from "vitest";

import { startCollectorInBackground } from "./telegram-collector-bootstrap.js";

describe("startCollectorInBackground", () => {
  it("starts collector asynchronously without blocking the caller", async () => {
    let started = false;
    let resolveStart!: () => void;
    const collectorService = {
      startFromCurrentSession: vi.fn(
        () =>
          new Promise<void>((resolve) => {
            resolveStart = () => {
              started = true;
              resolve();
            };
          })
      )
    };

    startCollectorInBackground(true, collectorService as never);

    expect(collectorService.startFromCurrentSession).toHaveBeenCalledTimes(1);
    expect(started).toBe(false);

    resolveStart();
    await Promise.resolve();

    expect(started).toBe(true);
  });

  it("does nothing when collector is disabled", () => {
    const collectorService = {
      startFromCurrentSession: vi.fn()
    };

    startCollectorInBackground(false, collectorService as never);

    expect(collectorService.startFromCurrentSession).not.toHaveBeenCalled();
  });

  it("swallows startup errors so the server can stay up", async () => {
    const collectorService = {
      startFromCurrentSession: vi.fn().mockRejectedValue(new Error("connect failed"))
    };

    expect(() => startCollectorInBackground(true, collectorService as never)).not.toThrow();
    await Promise.resolve();
    await Promise.resolve();

    expect(collectorService.startFromCurrentSession).toHaveBeenCalledTimes(1);
  });
});
