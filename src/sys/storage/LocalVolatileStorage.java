package sys.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Sets;

import api.Document;
import utils.Utils;

public class LocalVolatileStorage implements Storage {

	// Maps document ids to documents
	Map<String, Document> docs = new ConcurrentHashMap<>();
	
	// Maps each keyword to the set of documents that contain it
	Map<String, Set<Document>> reverseIndex = new ConcurrentHashMap<>();

	// Retrieves the list of urls of the documents that have all the keywords searched
	@Override
	public List<Document> search(List<String> keywords) {
		Utils.sleep(10);
		if (keywords.size() > 0) {
			Set<Document> res0 = reverseIndex.getOrDefault( keywords.get(0), Collections.emptySet() );
			Set<Document> res = keywords.subList(1, keywords.size())
					.stream()
					.map(word -> reverseIndex.getOrDefault(word, Collections.emptySet()))
					.reduce( res0, Sets::intersection);
			
			return new ArrayList<>(res);
		} else
			return Collections.emptyList();
	}

	//Adds the given document to the index
	@Override
	public boolean store(String id, Document doc) {
		Utils.sleep(5);
		if (docs.putIfAbsent(id, doc) == null) {
			doc.getKeywords().forEach(keyword -> {
				Set<Document> tmp = reverseIndex.putIfAbsent(keyword, Sets.newHashSet( doc ));
				if( tmp != null ) tmp.add(doc);
			});
			
			return true;
		} else
			return false;
	}

	//Removes a document from the index.
	@Override
	public boolean remove(String id) {
		Utils.sleep(5);
		Document doc = docs.remove(id);
		if (doc != null) {
			doc.getKeywords().forEach(keyword -> {
				reverseIndex.getOrDefault(keyword, new ConcurrentHashSet<>()).remove(doc);
			});
			return true;
		} else
			return false;
	}

}
