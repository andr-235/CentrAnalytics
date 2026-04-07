export type AuthMode = "login" | "register";

export type AuthPayload = {
  username: string;
  password: string;
};

export type AuthSuccess = {
  ok: true;
  token: string;
};

export type AuthFailure = {
  ok: false;
  error?: string;
  fieldErrors: Partial<Record<keyof AuthPayload, string>>;
};

export type AuthResult = AuthSuccess | AuthFailure;

export type SubmitAuth = (
  mode: AuthMode,
  payload: AuthPayload
) => Promise<AuthResult>;
