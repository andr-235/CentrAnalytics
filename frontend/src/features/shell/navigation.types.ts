export type PrimarySection =
  | "overview"
  | "vk"
  | "telegram"
  | "max"
  | "whatsapp"
  | "settings";

export type PlatformSection = Extract<
  PrimarySection,
  "vk" | "telegram" | "max" | "whatsapp"
>;

export type SecondarySection =
  | "messages"
  | "groups"
  | "collection"
  | "dialogs"
  | "session"
  | "sources"
  | "webhook";

export type NavigationSelection = {
  primary: PrimarySection;
  secondary: SecondarySection | null;
};

export const platformMenu = {
  vk: ["messages", "groups", "collection"],
  telegram: ["messages", "dialogs", "session"],
  max: ["messages", "sources"],
  whatsapp: ["messages", "webhook", "sources"]
} as const satisfies Record<PlatformSection, readonly SecondarySection[]>;
