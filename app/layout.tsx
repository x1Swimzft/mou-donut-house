import type { Metadata } from "next";
import { Noto_Sans_Thai } from "next/font/google";
import "./globals.css";

const noto = Noto_Sans_Thai({
  subsets: ["thai", "latin"],
  weight: ["400", "600", "700"],
  variable: "--font-noto",
  display: "swap"
});

export const metadata: Metadata = {
  title: "Mou Donut House — สั่งโดนัท",
  description: "ร้านโดนัทสำหรับลูกค้าประจำ สั่งแล้วมารับและชำระเงินที่ร้าน",
  other: {
    "color-scheme": "only light",
    "darkreader-lock": "true"
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="th" className={noto.variable}>
      <body>{children}</body>
    </html>
  );
}