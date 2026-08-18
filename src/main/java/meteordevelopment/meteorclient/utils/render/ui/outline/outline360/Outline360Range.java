package meteordevelopment.meteorclient.utils.render.ui.outline.outline360;

public record Outline360Range(float startDegrees, float endDegrees, int color) {
    
    public static Outline360Range of(float startDegrees, float endDegrees, int color) {
        return new Outline360Range(startDegrees, endDegrees, color);
    }
    
}
