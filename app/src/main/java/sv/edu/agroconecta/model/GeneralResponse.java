package sv.edu.agroconecta.model;

public class GeneralResponse {
    private boolean success;
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "GeneralResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
