import { NavLink, Outlet } from 'react-router-dom';

function Layout() {
  return (
    <div className="app">
      <header className="header">
        <div className="header-brand">
          <div className="logo">MTS</div>
          <h1>Money Transfer System</h1>
        </div>
        <nav className="header-nav">
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/accounts">Accounts</NavLink>
          <NavLink to="/transfer">Transfer</NavLink>
          <NavLink to="/transactions">Transactions</NavLink>
        </nav>
      </header>
      <main className="main">
        <Outlet />
      </main>
      <footer className="footer">
        <span>MTS v1.0</span>
        <span>Spring Boot + React + Node.js</span>
        <span>H2 In-Memory Database</span>
      </footer>
    </div>
  );
}

export default Layout;
