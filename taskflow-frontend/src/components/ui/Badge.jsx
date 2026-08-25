import "./Badge.css";

const STATUS_MAP = {
  ASSIGNED_NOT_STARTED: { label: "Not Started", cls: "badge-neutral" },
  STARTED_NOT_COMPLETED: { label: "In Progress", cls: "badge-warning" },
  COMPLETED: { label: "Completed", cls: "badge-success" },
  REMOVED: { label: "Removed", cls: "badge-danger" },
  PUBLISHED: { label: "Published", cls: "badge-success" },
  DRAFT: { label: "Draft", cls: "badge-neutral" },
};

/**
 * Badge / Status pill component
 * @param {object} props
 * @param {string} [props.status] - raw status key (auto maps label + color)
 * @param {"neutral"|"success"|"warning"|"danger"|"accent"|"purple"} [props.variant]
 * @param {string} [props.label] - custom label override
 * @param {React.ReactNode} [props.icon]
 */
export default function Badge({ status, variant, label, icon, className = "" }) {
  const mapped = status ? STATUS_MAP[status] : null;
  const cls = variant ? `badge-${variant}` : (mapped?.cls ?? "badge-neutral");
  const text = label ?? mapped?.label ?? (status ? status.replace(/_/g, " ") : "");

  return (
    <span className={`badge ${cls} ${className}`}>
      {icon && <span className="badge-icon">{icon}</span>}
      {text}
    </span>
  );
}
