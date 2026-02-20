export default function joinClasses(
  ...args: Array<string | null | undefined | false>
): string {
  return args.filter(Boolean).join(" ")
}
