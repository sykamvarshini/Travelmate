import re

# 1. Update Dashboard.jsx
dash_jsx_path = r"E:\travel\frontend\src\pages\Dashboard.jsx"
with open(dash_jsx_path, "r", encoding="utf-8") as f:
    jsx_content = f.read()

old_actions_block = """              <div className="trip-card-actions">
                <button 
                  onClick={() => handleDelete(trip.id)} 
                  className="delete-btn"
                  title="Delete trip permanently"
                  style={{background: 'none', border: 'none', color: '#ef4444', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 600, fontSize: '0.9rem'}}
                >
                  <Trash2 size={16} /> {t('dashboard.delete', 'Delete')}
                </button>

                <button 
                  onClick={() => handleOpenEdit(trip)} 
                  className="edit-btn" 
                  title="Edit trip inputs & preferences"
                  style={{background: 'none', border: 'none', color: '#0284c7', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 600, fontSize: '0.9rem'}}
                >
                  <Edit3 size={16} /> {t('dashboard.edit', 'Edit')}
                </button>

                <Link to={`/trip/${trip.id}`} className="view-btn text-gradient" style={{textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '5px', fontWeight: 600, fontSize: '0.9rem'}}>
                  {t('dashboard.view', 'View')} <ArrowRight size={16} />
                </Link>
              </div>"""

new_actions_block = """              <div className="trip-card-actions">
                <div className="actions-left">
                  <button 
                    onClick={() => handleDelete(trip.id)} 
                    className="trip-action-btn delete-btn"
                    title={t('dashboard.delete', 'Delete trip permanently')}
                  >
                    <Trash2 size={15} /> <span>{t('dashboard.delete', 'Delete')}</span>
                  </button>

                  <button 
                    onClick={() => handleOpenEdit(trip)} 
                    className="trip-action-btn edit-btn" 
                    title={t('dashboard.edit', 'Edit trip inputs & preferences')}
                  >
                    <Edit3 size={15} /> <span>{t('dashboard.edit', 'Edit')}</span>
                  </button>
                </div>

                <Link to={`/trip/${trip.id}`} className="trip-action-btn view-btn" title={t('dashboard.view', 'View interactive itinerary')}>
                  <span>{t('dashboard.view', 'View')}</span> <ArrowRight size={15} />
                </Link>
              </div>"""

if old_actions_block in jsx_content:
    jsx_content = jsx_content.replace(old_actions_block, new_actions_block)
    print("Replaced trip-card-actions in Dashboard.jsx successfully.")
else:
    print("Warning: old_actions_block not found in Dashboard.jsx directly.")

with open(dash_jsx_path, "w", encoding="utf-8") as f:
    f.write(jsx_content)


# 2. Update Dashboard.css
dash_css_path = r"E:\travel\frontend\src\pages\Dashboard.css"

new_css = """.dashboard-container {
  padding: 100px 2rem 4rem;
  max-width: 1240px;
  margin: 0 auto;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
  gap: 1.5rem;
}

.header-content h1 {
  font-size: 2.25rem;
  margin-bottom: 0.5rem;
}

.header-content p {
  color: var(--text-secondary, #475569);
  font-size: 1.05rem;
}

.trips-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.section-title {
  font-size: 1.5rem;
  color: var(--text-primary, #0f172a);
  margin-bottom: 1rem;
}

/* Trips Grid - Robust Responsive Layout */
.trips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 1.75rem;
  align-items: stretch;
}

/* Trip Card - Consistent Box Model & Height */
.trip-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 1.5rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 4px 12px -2px rgba(15, 23, 42, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  position: relative;
  overflow: hidden;
}

.trip-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 24px -4px rgba(15, 23, 42, 0.1);
  border-color: #cbd5e1;
}

/* Trip Card Header - Word Wrap & Status Alignment */
.trip-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.trip-card-header > div {
  min-width: 0;
  flex: 1;
}

.trip-card-header h3 {
  font-size: 1.2rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.35rem 0;
  line-height: 1.35;
  word-break: break-word;
  overflow-wrap: break-word;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-badge.active,
.status-planned {
  background: #eff6ff;
  color: #0284c7;
  border: 1px solid #bfdbfe;
}

/* Trip Card Body - Flexible Content Area */
.trip-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex: 1 1 auto;
}

.trip-detail {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  color: #475569;
  font-size: 0.92rem;
  line-height: 1.4;
  word-break: break-word;
}

.detail-icon {
  flex-shrink: 0;
  color: #0284c7;
}

.shared-indicator {
  color: #0369a1;
  font-weight: 500;
}

/* Trip Card Actions - Pinned to Bottom, Aligned & Responsive */
.trip-card-actions {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.trip-card-actions .actions-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.trip-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.5rem 0.85rem;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  text-decoration: none;
  line-height: 1;
  white-space: nowrap;
}

.trip-action-btn.delete-btn {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fee2e2;
}
.trip-action-btn.delete-btn:hover {
  background: #fee2e2;
  color: #b91c1c;
  border-color: #fca5a5;
}

.trip-action-btn.edit-btn {
  background: #f0f9ff;
  color: #0284c7;
  border: 1px solid #e0f2fe;
}
.trip-action-btn.edit-btn:hover {
  background: #e0f2fe;
  color: #0369a1;
  border-color: #bae6fd;
}

.trip-action-btn.view-btn {
  background: linear-gradient(135deg, #0284c7, #2563eb);
  color: #ffffff;
  border: 1px solid transparent;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.25);
  margin-left: auto;
}
.trip-action-btn.view-btn:hover {
  background: linear-gradient(135deg, #0369a1, #1d4ed8);
  box-shadow: 0 4px 10px rgba(2, 132, 199, 0.35);
  transform: translateY(-1px);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  grid-column: 1 / -1;
}

.empty-state h3 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: #64748b;
  margin-bottom: 1.5rem;
}

/* Trip Role Badges */
.trip-role-tag {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  padding: 2px 8px;
  border-radius: 9999px;
  margin-top: 4px;
}
.trip-role-tag.owner {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}
.trip-role-tag.editor {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}
.trip-role-tag.viewer {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.trip-last-update-note {
  font-size: 0.78rem;
  color: #0284c7;
  background: #f0f9ff;
  padding: 4px 8px;
  border-radius: 6px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* Toast */
.dashboard-toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: #0f172a;
  color: #ffffff;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.2);
  z-index: 1000;
}

/* Invitations Section */
.invitations-section {
  margin-bottom: 2.5rem;
}
.invitations-card {
  padding: 1.5rem;
  border: 1px solid #bae6fd;
  background: #f0f9ff;
  border-radius: 12px;
}
.invitations-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1rem;
}
.invitations-header h3 {
  font-size: 1.15rem;
  color: #0369a1;
  margin: 0;
}
.invitations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.invitation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e0f2fe;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
.invite-title {
  margin: 0 0 4px 0;
  font-size: 0.85rem;
  color: #64748b;
}
.invite-trip-name {
  font-size: 1rem;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 8px;
}
.invite-dest {
  color: #0284c7;
  font-size: 0.85rem;
}
.invite-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.82rem;
  color: #64748b;
  margin-top: 4px;
}
.invite-role-tag {
  background: #e2e8f0;
  color: #334155;
  font-size: 0.72rem;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}
.invitation-actions {
  display: flex;
  gap: 10px;
}
.btn-accept-invite {
  background: #0284c7;
  color: white;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.85rem;
  transition: background 0.2s;
}
.btn-accept-invite:hover {
  background: #0369a1;
}
.btn-decline-invite {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #cbd5e1;
  padding: 6px 14px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.85rem;
  transition: background 0.2s;
}
.btn-decline-invite:hover {
  background: #e2e8f0;
}

/* Edit Trip Modal */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  padding: 1rem;
}
.edit-trip-modal {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  padding: 2rem;
  position: relative;
}
.edit-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 1rem;
}
.edit-modal-header h2 {
  font-size: 1.4rem;
  color: #0f172a;
  margin: 0 0 4px 0;
}
.edit-modal-subtitle {
  font-size: 0.85rem;
  color: #64748b;
  margin: 0;
}
.modal-close-btn {
  background: #f1f5f9;
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1.1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}
.modal-close-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}
.edit-error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 1rem;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 8px;
}
.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
.form-grid-3 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 1rem;
}
.form-group {
  margin-bottom: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group label {
  font-size: 0.82rem;
  font-weight: 600;
  color: #334155;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
.form-group input,
.form-group select {
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.95rem;
  color: #0f172a;
  background: #ffffff;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.form-group input:focus,
.form-group select:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}
.interest-chips-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}
.interest-chip {
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  color: #475569;
  padding: 6px 14px;
  border-radius: 9999px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.interest-chip.active {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}
.edit-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid #f1f5f9;
}
.btn-primary {
  background: #0284c7;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover {
  background: #0369a1;
}
.btn-secondary {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #cbd5e1;
  padding: 10px 18px;
  border-radius: 8px;
  font-weight: 600;
  font-size: 0.95rem;
  cursor: pointer;
}
.btn-secondary:hover {
  background: #e2e8f0;
}

/* Mobile Responsiveness */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 85px 1.25rem 3rem;
  }
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  .trips-grid {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }
  .form-grid-2, .form-grid-3 {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .dashboard-container {
    padding: 80px 1rem 2.5rem;
  }
  .trip-card-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 0.65rem;
  }
  .trip-card-actions .actions-left {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
    width: 100%;
  }
  .trip-action-btn.view-btn {
    width: 100%;
    justify-content: center;
    margin-left: 0;
  }
}
"""

with open(dash_css_path, "w", encoding="utf-8") as f:
    f.write(new_css)

print("Dashboard.css updated successfully!")
