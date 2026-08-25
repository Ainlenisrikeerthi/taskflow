import "./LoadingSkeleton.css";

/**
 * Shimmer skeleton loader
 * @param {object} props
 * @param {"text"|"card"|"stat"|"table-row"|"rect"} [props.type="rect"]
 * @param {number} [props.count=1]
 * @param {string} [props.height]
 * @param {string} [props.width]
 * @param {string} [props.className]
 */
export default function LoadingSkeleton({
  type = "rect",
  count = 1,
  height,
  width,
  className = "",
}) {
  const items = Array.from({ length: count });

  if (type === "stat") {
    return (
      <div className="skeleton-stat-grid">
        {items.map((_, i) => (
          <div key={i} className="skeleton-stat-card">
            <div className="skeleton shimmer" style={{ width: 36, height: 36, borderRadius: 8 }} />
            <div className="skeleton shimmer" style={{ width: "50%", height: 32, borderRadius: 6, marginTop: 12 }} />
            <div className="skeleton shimmer" style={{ width: "70%", height: 12, borderRadius: 4, marginTop: 8 }} />
          </div>
        ))}
      </div>
    );
  }

  if (type === "card") {
    return (
      <div className="skeleton-card-grid">
        {items.map((_, i) => (
          <div key={i} className="skeleton-card">
            <div className="skeleton shimmer" style={{ width: "60%", height: 12, borderRadius: 4 }} />
            <div className="skeleton shimmer" style={{ width: "100%", height: 20, borderRadius: 6, marginTop: 12 }} />
            <div className="skeleton shimmer" style={{ width: "90%", height: 14, borderRadius: 4, marginTop: 8 }} />
            <div className="skeleton shimmer" style={{ width: "75%", height: 14, borderRadius: 4, marginTop: 6 }} />
            <div className="skeleton shimmer" style={{ width: "40%", height: 30, borderRadius: 8, marginTop: 20 }} />
          </div>
        ))}
      </div>
    );
  }

  if (type === "table-row") {
    return (
      <>
        {items.map((_, i) => (
          <tr key={i} className="skeleton-table-row">
            {Array.from({ length: 5 }).map((_, j) => (
              <td key={j} style={{ padding: "16px 18px" }}>
                <div className="skeleton shimmer" style={{ height: 14, borderRadius: 4 }} />
              </td>
            ))}
          </tr>
        ))}
      </>
    );
  }

  return (
    <div
      className={`skeleton shimmer ${className}`}
      style={{ height: height ?? 16, width: width ?? "100%", borderRadius: 6 }}
    />
  );
}
