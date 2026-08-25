import "./Input.css";

/**
 * Input / Textarea / Select form control
 * @param {object} props
 * @param {string} [props.label]
 * @param {string} [props.error]
 * @param {string} [props.hint]
 * @param {"input"|"textarea"|"select"} [props.as="input"]
 * @param {React.ReactNode} [props.prefixIcon]
 * @param {React.ReactNode} [props.suffix]
 */
export default function Input({
  label,
  error,
  hint,
  as: Tag = "input",
  prefixIcon,
  suffix,
  className = "",
  id,
  children,
  ...rest
}) {
  const inputId = id || (label ? label.replace(/\s+/g, "-").toLowerCase() : undefined);

  return (
    <div className={`input-field${error ? " input-error" : ""} ${className}`}>
      {label && <label className="input-label" htmlFor={inputId}>{label}</label>}

      <div className="input-wrapper">
        {prefixIcon && <span className="input-prefix">{prefixIcon}</span>}

        <Tag
          id={inputId}
          className={`input-control${prefixIcon ? " has-prefix" : ""}${suffix ? " has-suffix" : ""}`}
          {...rest}
        >
          {children}
        </Tag>

        {suffix && <span className="input-suffix">{suffix}</span>}
      </div>

      {error && <p className="input-error-msg">{error}</p>}
      {hint && !error && <p className="input-hint">{hint}</p>}
    </div>
  );
}
