"""Generate the 3 project diagrams with matplotlib (no external assets)."""
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch

OUT = "/home/hatch/workspace/job-search/project-14-insurance-claims/docs"


def box(ax, x, y, w, h, text, color="#dbeafe"):
    ax.add_patch(FancyBboxPatch((x, y), w, h, boxstyle="round,pad=0.02,rounding_size=0.03",
                                facecolor=color, edgecolor="#1e40af", linewidth=1.2))
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center", fontsize=9, weight="bold")


def arrow(ax, x1, y1, x2, y2, label=""):
    ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>",
                                 mutation_scale=12, color="#1e40af", linewidth=1.2))
    if label:
        ax.text((x1 + x2) / 2, (y1 + y2) / 2 + 0.06, label, ha="center", fontsize=8)


def new_fig():
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.set_xlim(0, 10)
    ax.set_ylim(0, 6)
    ax.axis("off")
    return fig, ax


# 1. Architecture — vertical chain: client → security → controllers →
# services → repositories → MySQL; services publish events via the interface.
fig, ax = new_fig()
ax.set_title("Insurance Claims System — Architecture", fontsize=13, weight="bold", pad=12)
box(ax, 0.3, 4.9, 2.4, 0.8, "React frontend (Vite)\n/ Postman\n(Basic auth demo/demo123)")
box(ax, 3.8, 4.9, 2.4, 0.8, "Spring Security\nHTTP Basic, in-memory user\n+ CORS for :5173")
box(ax, 7.3, 4.9, 2.4, 0.8, "Controllers\nCustomers · Policies · Claims")
box(ax, 7.3, 3.6, 2.4, 0.8, "Services\nCustomer · Policy · Claim\nPremiumCalculator")
box(ax, 7.3, 2.3, 2.4, 0.8, "Repositories\nSpring Data JPA")
box(ax, 7.3, 1.0, 2.4, 0.8, "MySQL 8 database\n(docker-compose)", color="#dcfce7")
box(ax, 3.8, 3.6, 2.4, 0.8, "EventPublisher\ninterface")
box(ax, 3.8, 2.3, 2.4, 0.8, "Local publisher\n(log + in-memory)\n— default", color="#dcfce7")
box(ax, 3.8, 1.0, 2.4, 0.8, "HTTP publisher\n(POST JSON, opt-in)", color="#fef9c3")
box(ax, 0.3, 3.6, 2.4, 0.8, "Validation\nBean Validation +\nservice rules")
arrow(ax, 2.7, 5.3, 3.8, 5.3)
arrow(ax, 6.2, 5.3, 7.3, 5.3)
arrow(ax, 8.5, 4.9, 8.5, 4.4)
arrow(ax, 8.5, 3.6, 8.5, 3.1)
arrow(ax, 8.5, 2.3, 8.5, 1.8)
arrow(ax, 7.3, 4.0, 6.2, 4.0)
arrow(ax, 5.0, 3.6, 5.0, 3.1)
arrow(ax, 5.0, 2.3, 5.0, 1.8)
ax.text(5, 0.3, "Validation applies at the controller (Bean Validation) and service (business rules) layers.",
        ha="center", fontsize=8)
fig.tight_layout()
fig.savefig(f"{OUT}/architecture.png", dpi=150)
plt.close(fig)

# 2. Policy + claim lifecycles
fig, ax = new_fig()
ax.set_title("Policy & Claim Lifecycles", fontsize=13, weight="bold", pad=12)
ax.text(5, 5.6, "POLICY", ha="center", fontsize=11, weight="bold")
box(ax, 0.3, 4.4, 1.8, 0.8, "PENDING", color="#fef9c3")
box(ax, 2.7, 4.4, 1.8, 0.8, "ACTIVE", color="#dcfce7")
box(ax, 5.1, 4.4, 1.8, 0.8, "RENEWED", color="#dbeafe")
box(ax, 7.5, 4.4, 2.2, 0.8, "EXPIRED / CANCELLED", color="#fee2e2")
arrow(ax, 2.1, 4.8, 2.7, 4.8)
arrow(ax, 4.5, 4.8, 5.1, 4.8)
arrow(ax, 6.9, 4.8, 7.5, 4.8)
ax.text(5, 3.9, "Illegal transitions (e.g. ACTIVE → PENDING) are rejected with 400.", ha="center", fontsize=8)
ax.text(5, 3.3, "CLAIM", ha="center", fontsize=11, weight="bold")
box(ax, 0.3, 2.1, 1.9, 0.8, "REGISTERED", color="#fef9c3")
box(ax, 2.8, 2.1, 1.9, 0.8, "UNDER_REVIEW", color="#dbeafe")
box(ax, 5.3, 2.1, 1.9, 0.8, "APPROVED", color="#dcfce7")
box(ax, 7.8, 2.1, 1.9, 0.8, "SETTLED", color="#bbf7d0")
box(ax, 2.8, 0.8, 1.9, 0.8, "REJECTED", color="#fee2e2")
arrow(ax, 2.2, 2.5, 2.8, 2.5)
arrow(ax, 4.7, 2.5, 5.3, 2.5)
arrow(ax, 7.2, 2.5, 7.8, 2.5)
arrow(ax, 3.75, 2.1, 3.75, 1.6)
ax.text(5, 0.3, "Settle pays min(claimAmount, remainingCoverage). REJECTED (from REGISTERED or UNDER_REVIEW) / SETTLED are terminal.",
        ha="center", fontsize=8)
fig.tight_layout()
fig.savefig(f"{OUT}/policy-claim-lifecycle.png", dpi=150)
plt.close(fig)

# 3. Event flow
fig, ax = new_fig()
ax.set_title("Domain Event Flow", fontsize=13, weight="bold", pad=12)
box(ax, 0.3, 4.5, 2.4, 1.0, "Business operation\n(policy created,\nclaim settled, ...)")
box(ax, 3.7, 4.5, 2.6, 1.0, "Service builds\na DomainEvent\n(type, entity, details)")
box(ax, 7.3, 4.5, 2.4, 1.0, "EventPublisher\ninterface")
box(ax, 1.2, 2.4, 3.0, 1.0, "LocalEventPublisher\nlog + in-memory list\n(DEFAULT — no setup)", color="#dcfce7")
box(ax, 5.8, 2.4, 3.0, 1.0, "HttpEventPublisher\nPOST JSON to endpoint\n(opt-in via config)", color="#fef9c3")
box(ax, 3.2, 0.7, 3.6, 1.0, "Downstream consumers\nnotifications · analytics\n(not part of this service)", color="#fee2e2")
arrow(ax, 2.7, 5.0, 3.7, 5.0)
arrow(ax, 6.3, 5.0, 7.3, 5.0)
arrow(ax, 8.5, 4.5, 7.3, 3.4)
arrow(ax, 8.5, 4.5, 4.2, 3.4)
arrow(ax, 2.7, 2.4, 3.8, 1.7)
arrow(ax, 7.3, 2.4, 6.2, 1.7)
ax.text(5, 0.2, "Delivery failures are logged, never fatal to the business operation.",
        ha="center", fontsize=8)
fig.tight_layout()
fig.savefig(f"{OUT}/event-flow.png", dpi=150)
plt.close(fig)

print("diagrams written")
