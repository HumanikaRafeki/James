package humanika.rafeki.james.phrases;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import me.mcofficer.esparser.DataNode;

class Replacements implements PhraseExpander {
    private LinkedHashMap<String, String> toReplace;

    public Replacements(DataNode node) {
        toReplace = new LinkedHashMap<String, String>();
        for(DataNode child : node.getChildren()) {
            ArrayList<String> tokens = child.getTokens();
            if(tokens.size() < 1 || tokens.get(0).length() < 1)
                continue;
            String value = tokens.size() > 1 ? tokens.get(1) : "";
            toReplace.put(tokens.get(0), value);
        }
    }

    @Override
    public void expand(StringBuilder result, PhraseProvider phrases, Set<String> touched, PhraseLimits limits) {
        for(Map.Entry<String, String> entry : toReplace.entrySet()) {
            String key = entry.getKey();
            int keyLen = key.length();
            if(keyLen < 1)
                continue;
            String value = entry.getValue();
            int valueLen = value.length();
            int point = 0;
            while(point < result.length()) {
                int next = result.indexOf(key, point);
                if(next < 0)
                    break;
                int change = valueLen - keyLen;
                if(change > 0 && limits.canExpandBy(change, result)) {
                    result.replace(next, next + keyLen, value);
                    point = next + valueLen;
                } else if(change <= 0) {
                    result.replace(next, next + keyLen, value);
                    point = next + keyLen;
                }
            }
        }
    }
}
