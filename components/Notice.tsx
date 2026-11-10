export default function Notice({
  message,
  tone = "error"
}: {
  message: string;
  tone?: "error" | "info";
}) {
  return (
    <div className={`notice${tone === "info" ? " info" : ""}`} role={tone === "error" ? "alert" : "status"}>
      {message}
    </div>
  );
}