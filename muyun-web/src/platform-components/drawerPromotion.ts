export interface DrawerPromotion {
  title?: string;
  promote(): void | Promise<void>;
}
