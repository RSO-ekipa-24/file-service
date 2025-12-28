package essa.dto.tag;

import java.util.ArrayList;
import java.util.List;

public class TagGetResponse {
    private List<String> systemTags;

    private List<String> userTags;

    public TagGetResponse() {}

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public void addSystemTag(String tag) {
        if (this.systemTags == null) {
            this.systemTags = new ArrayList<>();
        }
        this.systemTags.add(tag);
    }

    public List<String> getUserTags() {
        return userTags;
    }

    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    public void addUserTag(String tag) {
        if (this.userTags == null) {
            this.userTags = new ArrayList<>();
        }
        this.userTags.add(tag);
    }
}
