package loom_compatibility;

public class LoomUnavailable extends Exception {
    LoomUnavailable(LinkageError cause) {
        super("Loom is not available.", cause);
    }
}
