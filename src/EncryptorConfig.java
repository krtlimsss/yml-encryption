import java.util.List;

public class EncryptorConfig {

    private String path;
    private List<String> properties;
    private String key;

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public List<String> getProperties() { return properties; }
    public void setProperties(List<String> properties) { this.properties = properties; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
