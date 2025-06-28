export interface ForeGroundLocationPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
