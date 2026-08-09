import Sidebar from "./Sidebar";
import Header from "./Header";

export default function Layout({
  activePage,
  onNavigate,
  title,
  children,
}) {
  return (
    <div className="app-layout">
      <Sidebar
        activePage={activePage}
        onNavigate={onNavigate}
      />

      <main className="app-main">
        <Header title={title} />

        <div className="app-content">
          {children}
        </div>
      </main>
    </div>
  );
}