import { Link } from "react-router-dom";
import { ListChecks, Activity, Link2 } from "lucide-react";

export default function Landing() {
  return (
    <main className="landing-premium">
      <section className="hero hero-centered">
        <div className="page-container hero-centered-inner">
          <span className="eyebrow">Task assignment and progress tracking</span>
          <h1>Keep every task clear, visible, and on track.</h1>
          <p>
            TaskFlow brings task assignment, progress updates, proof submission, and work history into one organized workspace so users always know what to do and administrators always know what is happening.
          </p>
          <div className="hero-actions hero-actions-centered">
            <Link to="/register" className="primary-button hero-btn-primary">
              Get Started
            </Link>
          </div>
        </div>
      </section>

      <section className="section features" id="features">
        <div className="page-container">
          <h2>Everything you need to manage tasks</h2>
          <p>
            A simple workflow for assigning work, following progress, and keeping completion records in one place.
          </p>
          <div className="feature-grid">
            <Feature
              icon={<ListChecks />}
              title="Simple Task Assignment"
              text="Administrators publish tasks in one place and users can take available work without depending on scattered messages or manual tracking."
            />
            <Feature
              icon={<Activity />}
              title="Clear Progress Tracking"
              text="Follow each assignment from Not Started to In Progress and Completed with current status information available to both users and administrators."
            />
            <Feature
              icon={<Link2 />}
              title="Proof & Work History"
              text="Users can submit proof links for completed work while TaskFlow preserves assignment history for clear and reliable records."
            />
          </div>
        </div>
      </section>

      <section className="section about-section" id="about">
        <div className="page-container about-premium">
          <h2>About TaskFlow</h2>
          <p>
            TaskFlow is built to make task coordination simple and transparent. Instead of losing assignments inside chats or group messages, every task has a clear status, responsible user, proof of work, and history. Administrators can monitor activity from one workspace, while users can clearly see what they are working on and what they have already completed.
          </p>
        </div>
      </section>

      <footer className="footer">
        <div className="page-container footer-inner">
          <strong>TaskFlow</strong>
          <span>Clear tasks. Visible progress.</span>
        </div>
      </footer>
    </main>
  );
}

function Feature({ icon, title, text }) {
  return (
    <article className="feature-card">
      <div className="feature-icon">{icon}</div>
      <h3>{title}</h3>
      <p>{text}</p>
    </article>
  );
}
