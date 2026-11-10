import { formatMoney } from "@/lib/format";
import type { Product } from "@/lib/types";
import ProductArt from "./ProductArt";

export default function ProductCard({
  product,
  index,
  onAdd,
  cartQty = 0
}: {
  product: Product;
  index: number;
  onAdd: (product: Product, amount: number) => void;
  cartQty?: number;
}) {
  const soldOut = !product.is_available;
  return (
    <article className={`product${soldOut ? " sold-out" : ""}`}>
      <div className="product-media-wrap">
        <div className="product-media">
          <ProductArt category={product.category} index={index} muted={soldOut} />
        </div>
        {soldOut ? (
          <span className="sold-out-badge">หมดชั่วคราว</span>
        ) : cartQty > 0 ? (
          <span className="cart-qty-badge">ในตะกร้า {cartQty} ชิ้น</span>
        ) : null}
      </div>

      <div className="product-info">
        <h3 className="product-title">{product.name}</h3>
        <p className="price">{formatMoney(product.price)}</p>
      </div>

      {soldOut ? (
        <div className="product-actions">
          <button className="button disabled" type="button" disabled>
            สินค้าหมดชั่วคราว
          </button>
        </div>
      ) : (
        <div className="product-actions add-buttons-group">
          <button
            className="button primary batch-btn"
            type="button"
            onClick={() => onAdd(product, 5)}
            aria-label={`เพิ่ม ${product.name} 5 ชิ้น`}
          >
            + 5 ชิ้น
          </button>
          <button
            className="button primary batch-btn"
            type="button"
            onClick={() => onAdd(product, 10)}
            aria-label={`เพิ่ม ${product.name} 10 ชิ้น`}
          >
            + 10 ชิ้น
          </button>
        </div>
      )}
    </article>
  );
}
