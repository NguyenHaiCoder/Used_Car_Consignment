package fu.he.training.fall2026.model.ui;

import java.util.List;

/** Read models for the UI preview. They are not persistence entities. */
public final class UiModels {
    private UiModels() { }
    public record Role(String key, String label, String userName, String initials) { }
    public record Link(String label, String url, String module) { }
    public record RoleLinks(Role role, List<Link> links) { }
    public record Field(String name, String label, String type, boolean required,
                        String value, List<String> options, String hint) { }
    public record Fact(String label, String value) { }
    public record Check(String label, boolean passed, String explanation) { }
    public record Row(Long id, List<String> cells, String status, String url) { }
    public record Metric(String label, String value, String caption) { }
    public record Activity(String time, String actor, String action) { }
    public record Action(String label, String url, String kind, boolean enabled, String reason) { }
    public record Screen(String key, String title, String module, String description, String kind,
                         String status, String notice, List<Field> fields, List<String> columns,
                         List<Row> rows, List<Fact> facts, List<Check> checks, List<Action> actions,
                         List<Activity> history, List<Metric> metrics, List<Link> steps) { }
}
