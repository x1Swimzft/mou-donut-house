export function formatMoney(value: number | string): string {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "0 บาท";
  const hasSatang = Math.abs(amount - Math.round(amount)) > 0.0001;
  return `${amount.toLocaleString("th-TH", {
    minimumFractionDigits: hasSatang ? 2 : 0,
    maximumFractionDigits: 2
  })} บาท`;
}

export function shortOrderId(orderId: string): string {
  return orderId.slice(0, 8).toUpperCase();
}