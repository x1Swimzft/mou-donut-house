export default function ProductArt({
  category,
  index = 0,
  muted = false
}: {
  category: string | null;
  index?: number;
  muted?: boolean;
}) {
  if (category === "drink") {
    const drinks = [
      { cup: "#D98C4A", fill: "#F3D3B0", straw: "#8E1B3A" },
      { cup: "#F3D3B0", fill: "#6B4226", straw: "#8E1B3A" },
      { cup: "#D9E8F0", fill: "#9FC4D8", straw: "#8E1B3A" }
    ];
    const drink = muted
      ? { cup: "#B9AEA7", fill: "#8F857F", straw: "#6B625C" }
      : drinks[index % drinks.length];
    return (
      <svg viewBox="0 0 100 100" aria-hidden="true" focusable="false">
        <path d="M30 28h40l-4 58c-.5 6-5 9-16 9s-15.5-3-16-9l-4-58Z" fill={drink.cup} />
        <rect x="26" y="22" width="48" height="10" rx="4" fill="#2A1712" />
        <rect x="46" y="8" width="8" height="18" rx="4" fill={drink.straw} />
        <path d="M36 40h28l-2 24c-.4 5-4 8-12 8s-11.6-3-12-8l-2-24Z" fill={drink.fill} opacity=".55" />
      </svg>
    );
  }

  const glazes = muted
    ? ["#8F857F"]
    : ["#8E1B3A", "#4A2C1D", "#A8324F"];
  const glaze = glazes[index % glazes.length];
  const dough = muted ? "#B9AEA7" : "#D99A5B";
  const hole = muted ? "#EFE7E1" : "#FFF6F2";
  const sprinkles = muted ? ["#EFE7E1"] : ["#FFF6F2", "#F2C14E"];
  return (
    <svg viewBox="0 0 100 100" aria-hidden="true" focusable="false">
      <circle cx="50" cy="52" r="36" fill={dough} />
      <circle cx="50" cy="52" r="16" fill={hole} />
      <path d="M22 42c8-14 20-20 28-20s20 6 28 20c-6 6-14 8-28 8s-22-2-28-8Z" fill={glaze} />
      <circle cx="38" cy="38" r="2.5" fill={sprinkles[0]} />
      <circle cx="58" cy="35" r="2.5" fill={sprinkles[1]} />
      <circle cx="66" cy="46" r="2.5" fill={sprinkles[0]} />
      <circle cx="32" cy="48" r="2.5" fill={sprinkles[1]} />
    </svg>
  );
}