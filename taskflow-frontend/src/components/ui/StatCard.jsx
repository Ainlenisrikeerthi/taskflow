import "./StatCard.css";

/**
 * Premium metric card
 * @param {object} props
 * @param {React.ReactNode} props.icon
 * @param {string} props.label
 * @param {string|number} props.value
 * @param {"accent"|"success"|"warning"|"danger"|"neutral"} [props.color="accent"]
 * @param {string} [props.subtitle]
 */
export default function StatCard({ icon, label, value, color = "neutral", subtitle }) {
  return (
    <div className={`stat-card stat-card-${color}`}>
      <div className="stat-card-header">
        <div className={`stat-card-icon stat-icon-${color}`}>{icon}</div>
      </div>
      <div className="stat-card-value">{value ?? "—"}</div>
      <div className="stat-card-label">{label}</div>
      {subtitle && <div className="stat-card-subtitle">{subtitle}</div>}
    </div>
  );
}
