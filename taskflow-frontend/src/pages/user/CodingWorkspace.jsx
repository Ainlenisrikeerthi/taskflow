import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../../data/api";
import Button from "../../components/ui/Button";
import { Play, ChevronLeft, Trophy, CheckCircle, XCircle, AlertCircle, Code2 } from "lucide-react";

export default function CodingWorkspace() {
  const { id } = useParams();
  const [task, setTask] = useState(null);
  const [tests, setTests] = useState([]);
  const [assignment, setAssignment] = useState(null);
  const [language, setLanguage] = useState("java");
  const [code, setCode] = useState("");
  const [runResult, setRunResult] = useState(null);
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [assigning, setAssigning] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { load(); }, [id]);

  async function load() {
    setError("");
    try {
      const [t, visible, active] = await Promise.all([
        api.tasks.getById(id),
        api.coding.getVisibleTests(id),
        api.assignments.getMyActive(),
      ]);
      setTask(t);
      setCode(t.starterCode || "");
      setTests(visible || []);
      setAssignment(active.find((a) => a.taskId === Number(id)) || null);
    } catch (e) {
      setError(e.message || "Failed to load coding workspace.");
    }
  }

  async function assignAndStart() {
    setAssigning(true);
    setError("");
    try {
      const a = await api.assignments.assign(Number(id));
      setAssignment(a);
    } catch (e) {
      setError(e.message || "Failed to assign coding task.");
    } finally {
      setAssigning(false);
    }
  }

  async function runVisibleTests() {
    setBusy(true);
    setError("");
    setRunResult(null);
    try {
      setRunResult(await api.coding.run(id, language, code));
    } catch (e) {
      setError(e.message || "Unable to run visible tests.");
    } finally {
      setBusy(false);
    }
  }

  async function submit() {
    setBusy(true);
    setError("");
    setResult(null);
    try {
      const response = await api.coding.submit(id, language, code);
      setResult(response);
      await load();
    } catch (e) {
      setError(e.message || "Unable to submit solution.");
    } finally {
      setBusy(false);
    }
  }

  if (!task) return <div>{error || "Loading coding workspace..."}</div>;

  return (
    <div>
      <Link to="/user/tasks" className="coding-back"><ChevronLeft size={15} />Back to tasks</Link>

      <div className="page-header">
        <div className="page-header-left">
          <h1>{task.title}</h1>
          <p>{task.difficulty || "Coding"} · scored out of 5 using test correctness + efficiency + code quality</p>
        </div>
        <Link to="/user/leaderboard"><Button variant="secondary" icon={<Trophy size={15} />}>Leaderboard</Button></Link>
      </div>

      {error && <div className="alert-box danger" style={{ marginBottom: 18 }}><AlertCircle size={16} /><span>{error}</span></div>}

      {!assignment ? (
        <div className="card coding-assign-card">
          <Code2 size={34} />
          <h3>Assign this coding task to start</h3>
          <p>This is a coding task, so you will submit code here instead of a proof URL.</p>
          <Button variant="primary" loading={assigning} onClick={assignAndStart}>Assign & Start Coding</Button>
        </div>
      ) : (
        <div className="coding-workspace">
          <section className="card coding-problem">
            <h3>Problem</h3>
            <p>{task.description}</p>
            <h4>Instructions</h4>
            <p>{task.instructions}</p>
            <h4>Visible Test Cases</h4>
            {tests.length === 0 ? <p>No visible test cases were published.</p> : tests.map((t, i) => (
              <div className="coding-example" key={`${t.input}|${t.expectedOutput}`}>
                <strong>Case {i + 1}</strong>
                <pre>{`Input:\n${t.input}\n\nExpected:\n${t.expectedOutput}`}</pre>
              </div>
            ))}
          </section>

          <section className="card coding-editor-panel">
            <div className="coding-editor-head">
              <div><h3>Your Solution</h3><span className="coding-editor-note">Run checks visible cases. Submit evaluates all visible + hidden cases; OpenRouter agent analyzes efficiency and code quality.</span></div>
              <select value={language} onChange={(e) => setLanguage(e.target.value)} aria-label="Programming language">
                <option value="java">Java 17</option>
                <option value="python">Python 3</option>
                <option value="javascript">JavaScript (Node.js)</option>
                <option value="c">C (GCC)</option>
                <option value="cpp">C++ 17</option>
              </select>
            </div>

            <textarea className="coding-editor" spellCheck="false" value={code} onChange={(e) => setCode(e.target.value)} />

            <div className="coding-run-actions">
              <Button variant="secondary" loading={busy} icon={<Play size={15} />} onClick={runVisibleTests}>Run Visible Tests</Button>
              <Button variant="primary" loading={busy} onClick={submit}>Submit & Evaluate /5</Button>
            </div>

            {runResult && (
              <div className="coding-run-results">
                <h4>{runResult.passed}/{runResult.total} visible tests passed</h4>
                {runResult.cases?.map((c) => (
                  <div className={`coding-run-case ${c.passed ? "pass" : "fail"}`} key={c.caseNumber}>
                    <div className="coding-run-case-title">{c.passed ? <CheckCircle size={15} /> : <XCircle size={15} />} Case {c.caseNumber}</div>
                    {!c.passed && <pre>{[
                      `Expected: ${c.expectedOutput}`,
                      `Actual: ${c.actualOutput || "(no output)"}`,
                      c.error ? `Error: ${c.error}` : null,
                    ].filter(Boolean).join("\n")}</pre>}
                  </div>
                ))}
              </div>
            )}

            {result && (
              <div className="coding-score">
                <div className="coding-score-number">{result.score}<small>/5</small></div>
                <div>
                  <strong>Marks</strong>
                  <p>{result.feedback || "No feedback was provided."}</p>
                </div>
              </div>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
