package jdbcrunner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code Template}
 * オブジェクトをLRUアルゴリズムを用いてキャッシュするクラスです。
 *
 * @author Sadao Hiratsuka
 */
public class TemplateCache extends LinkedHashMap<String, Template> {
	private static final long serialVersionUID = 1L;
	private static final int TEMPLATE_CACHE_SIZE = 100;

	/**
	 * {@code TemplateCache}を構築します。
	 */
	public TemplateCache() {
		super(16, 0.75f, true);
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<String, Template> eldest) {
		if (size() > TEMPLATE_CACHE_SIZE) {
			return true;
		} else {
			return false;
		}
	}
}
