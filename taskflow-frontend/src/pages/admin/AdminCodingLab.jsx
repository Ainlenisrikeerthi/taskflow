import { useMemo, useState } from "react";
import { api } from "../../data/api";
import Button from "../../components/ui/Button";
import Input from "../../components/ui/Input";
import { Sparkles, Plus, Trash2, Save, Eye, EyeOff, CheckCircle, AlertCircle } from "lucide-react";

export default function AdminCodingLab() {
  const [title, setTitle] = useState("");
  const [difficulty, setDifficulty] = useState("MEDIUM");
  const [language, setLanguage] = useState("java");
  const [deadline, setDeadline] = useState("");
  const [description, setDescription] = useState("");
  const [instructions, setInstructions] = useState("");
  const [starterCode, setStarterCode] = useState("");
  const [tests, setTests] = useState([]);
  const [msg, setMsg] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const visibleCount = useMemo(() => tests.filter((t) => !t.hidden).length, [tests]);
  const hiddenCount = useMemo(() => tests.filter((t) => t.hidden).length, [tests]);

  function resetBuilder() {
    setTitle("");
    setDifficulty("MEDIUM");
    setLanguage("java");
    setDeadline("");
    setDescription("");
    setInstructions("");
    setStarterCode("");
    setTests([]);
  }

  async function generate() {
    setLoading(true);
    setMsg("");
    setError("");
    try {
      if (!title.trim()) throw new Error("Enter a problem title before using Auto Generate.");
      const x = await api.coding.generate({ title, difficulty, language });
      setDescription(x.description || "");
      setInstructions(x.instructions || "");
      setStarterCode(x.starterCode || "");
      setTests(Array.isArray(x.testCases) ? x.testCases : []);
      setMsg(`Draft generated with ${x.testCases?.length || 0} test cases. Review every field before saving or publishing.`);
    } catch (e) {
      setError(e.message || "Failed to generate coding task draft.");
    } finally {
      setLoading(false);
    }
  }

  function validate() {
    if (!title.trim()) return "Problem title is required.";
    if (!deadline) return "Deadline is required.";
    if (!description.trim()) return "Problem description is required.";
    if (tests.length === 0) return "At least one test case is required.";
    const invalid = tests.findIndex((t) => !String(t.input || "").trim() || !String(t.expectedOutput || "").trim());
    if (invalid >= 0) return `Test case ${invalid + 1} needs both input and expected output.`;
    return "";
  }

  async function save(status) {
    if (saving) return;
    setMsg("");
    setError("");
    const problem = validate();
    if (problem) {
      setError(problem);
      return;
    }
    setSaving(true);
    try {
      await api.coding.saveTask({
        title,
        difficulty,
        deadline,
        description,
        instructions,
        starterCode,
        status,
        testCases: tests,
      });
      if (status === "PUBLISHED") {
        resetBuilder();
        setMsg("Coding task and test cases were published successfully. The builder has been cleared for the next question.");
      } else {
        setMsg("Coding task and test cases were saved as a draft.");
      }
    } catch (e) {
      setError(e.message || "Failed to save coding task.");
    } finally {
      setSaving(false);
    }
  }

  function update(i, key, value) {
    setTests((current) => current.map((x, n) => (n === i ? { ...x, [key]: value } : x)));
  }

  return (
    <div>
      <div className="page-header">
        <div className="page-header-left">
          <div className="breadcrumbs"><span>Admin</span><span>/</span><span>Coding Lab</span></div>
          <h1>AI Coding Task Builder</h1>
          <p>Generate a draft, inspect visible and hidden test cases, edit anything you want, then save or publish.</p>
        </div>
      </div>

      {error && <div className="alert-box danger" style={{ marginBottom: 20 }}><AlertCircle size={16} /><span>{error}</span></div>}
      {msg && <div className="alert-box success" style={{ marginBottom: 20 }}><CheckCircle size={16} /><span>{msg}</span></div>}

      <div className="card coding-builder">
        <div className="coding-form-grid">
          <Input label="Problem title" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Two Sum" />
          <Input label="Deadline" type="date" value={deadline} onChange={(e) => setDeadline(e.target.value)} />
          <Input label="Difficulty" as="select" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
            <option>EASY</option><option>MEDIUM</option><option>HARD</option>
          </Input>
          <Input label="Language" as="select" value={language} onChange={(e) => setLanguage(e.target.value)}>
            <option value="java">Java 17</option><option value="python">Python 3</option>
            <option value="javascript">JavaScript (Node.js)</option><option value="c">C (GCC)</option>
            <option value="cpp">C++ 17</option>
          </Input>
        </div>

        <div className="coding-generate-row">
          <Button onClick={generate} loading={loading} variant="primary" icon={<Sparkles size={16} />}>Auto Generate Draft</Button>
          <span className="coding-helper-text">The generated content is only a draft. Admin review is required before publishing.</span>
        </div>

        <label className="coding-label">Description<textarea className="coding-textarea" value={description} onChange={(e) => setDescription(e.target.value)} /></label>
        <label className="coding-label">Instructions / constraints<textarea className="coding-textarea" value={instructions} onChange={(e) => setInstructions(e.target.value)} /></label>
        <label className="coding-label">Starter code<textarea className="coding-code" value={starterCode} onChange={(e) => setStarterCode(e.target.value)} /></label>

        <div className="coding-test-head">
          <div>
            <h3>Test Cases</h3>
            <p className="coding-test-summary">{tests.length} total · {visibleCount} visible to users · {hiddenCount} hidden for final evaluation</p>
          </div>
          <Button variant="secondary" size="sm" icon={<Plus size={14} />} onClick={() => setTests([...tests, { input: "", expectedOutput: "", hidden: true }])}>Add case</Button>
        </div>

        {tests.length === 0 ? (
          <div className="coding-empty-tests">No test cases yet. Click <strong>Auto Generate Draft</strong> or add a case manually.</div>
        ) : tests.map((t, i) => (
          <div className="coding-test-card" key={`${t.input}|${t.expectedOutput}|${t.hidden}`}>
            <div className="coding-test-card-head">
              <strong>Case {i + 1}</strong>
              <span className={t.hidden ? "test-badge hidden" : "test-badge visible"}>{t.hidden ? <EyeOff size={13} /> : <Eye size={13} />}{t.hidden ? "Hidden" : "Visible"}</span>
            </div>
            <div className="coding-test-inputs">
              <label>Input<textarea placeholder="stdin" value={t.input || ""} onChange={(e) => update(i, "input", e.target.value)} /></label>
              <label>Expected output<textarea placeholder="expected stdout" value={t.expectedOutput || ""} onChange={(e) => update(i, "expectedOutput", e.target.value)} /></label>
            </div>
            <div className="coding-test-card-footer">
              <label className="coding-hidden-toggle"><input type="checkbox" checked={!!t.hidden} onChange={(e) => update(i, "hidden", e.target.checked)} /> Hidden during user practice</label>
              <button type="button" className="icon-button" onClick={() => setTests(tests.filter((_, n) => n !== i))} aria-label={`Delete case ${i + 1}`}><Trash2 size={15} /></button>
            </div>
          </div>
        ))}

        <div className="coding-actions">
          <Button variant="secondary" icon={<Save size={15} />} loading={saving} disabled={saving} onClick={() => save("DRAFT")}>Save Draft</Button>
          <Button variant="primary" loading={saving} disabled={saving} onClick={() => save("PUBLISHED")}>Publish After Review</Button>
        </div>
      </div>
    </div>
  );
}
