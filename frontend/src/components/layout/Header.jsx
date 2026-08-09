export default function Header({ title }) {
  return (
    <header className="app-header">
      <div>
        <span className="header-label">CONTROL CENTER</span>
        <h1>{title}</h1>
      </div>

      <div className="header-user">
        <span>Admin</span>
      </div>
    </header>
  );
}