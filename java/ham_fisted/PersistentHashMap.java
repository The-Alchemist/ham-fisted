package ham_fisted;

import static ham_fisted.HashBase.*;
import clojure.lang.APersistentMap;
import clojure.lang.Util;
import clojure.lang.MapEntry;
import clojure.lang.IMapEntry;
import clojure.lang.IPersistentMap;
import clojure.lang.IPersistentCollection;
import clojure.lang.IteratorSeq;
import clojure.lang.IEditableCollection;
import clojure.lang.ISeq;
import clojure.lang.IObj;
import clojure.lang.IKVReduce;
import clojure.lang.RT;
import clojure.lang.IDeref;
import clojure.lang.IFn;
import java.util.Iterator;
import java.util.Collection;
import java.util.Set;
import java.util.Objects;
import java.util.Map;


public class PersistentHashMap
  extends APersistentMap
  implements IObj, IKVReduce, IEditableCollection {

  final HashBase hb;

  public static final HashProvider equivHashProvider = new HashProvider(){
      public int hash(Object obj) {
	return Util.hasheq(obj);
      }
      public boolean equals(Object lhs, Object rhs) {
	return Util.equiv(lhs,rhs);
      }
    };

  public PersistentHashMap() {
    hb = new HashBase(equivHashProvider);
  }
  public PersistentHashMap(HashBase hm) {
    hb = hm;
  }
  public PersistentHashMap(HashProvider _hp, boolean assoc, Object... kvs) {
    HashMap<Object,Object> hm = new HashMap<Object,Object>(_hp);
    final int nkvs = kvs.length;
    if (0 != (nkvs % 2))
      throw new RuntimeException("Uneven number of keyvals");
    final int nks = nkvs / 2;
    for (int idx = 0; idx < nks; ++idx) {
      final int kidx = idx * 2;
      final int vidx = kidx + 1;
      hm.put(kvs[kidx], kvs[vidx]);
    }
    if (assoc == false && hm.size() != nks)
      throw new RuntimeException("Duplicate key detected: " + String.valueOf(kvs));
    hb = hm;
  }
  public PersistentHashMap(boolean assoc, Object... kvs) {
    this(equivHashProvider, assoc, kvs);
  }
  public boolean containsKey(Object key) {
    return hb.containsKey(key);
  }
  public boolean containsValue(Object v) {
    return hb.containsValue(v);
  }
  public int size() { return hb.size(); }
  public int count() { return hb.size(); }
  public Set keySet() { return hb.keySet((Object)null, false); }
  public Set entrySet() { return hb.entrySet((Map.Entry<Object,Object>)null, false); }
  public Collection values() { return hb.values((Object)null, false); }
  public IMapEntry entryAt(Object key) {
    final LeafNode node = hb.getNode(key);
    return node != null ? MapEntry.create(key, node.val()) : null;
  }
  public ISeq seq() { return  IteratorSeq.create(iterator()); }
  public Object valAt(Object key, Object notFound) {
    return hb.getOrDefaultImpl(key, notFound);
  }
  public Object valAt(Object key){
    return hb.getOrDefaultImpl(key, null);
  }
  public Iterator iterator(){
    return hb.iterator(entryIterFn);
  }

  public Iterator keyIterator(){
    return hb.iterator(keyIterFn);
  }

  public Iterator valIterator() {
    return hb.iterator(valIterFn);
  }

  public IPersistentMap assoc(Object key, Object val) {
    return new PersistentHashMap(hb.shallowClone().assoc(key, val));
  }
  public IPersistentMap assocEx(Object key, Object val) {
    if(containsKey(key))
      throw new RuntimeException("Key already present");
    return assoc(key, val);
  }
  public IPersistentMap without(Object key) {
    if (hb.c.count() == 0 || (key == null && hb.nullEntry == null))
      return this;
    return new PersistentHashMap(hb.shallowClone().dissoc(key));
  }
  public static PersistentHashMap EMPTY = new PersistentHashMap(new HashBase(equivHashProvider));
  public IPersistentCollection empty() {
    return (IPersistentCollection)EMPTY.withMeta(hb.meta);
  }
  public IPersistentMap meta() { return hb.meta; }
  public IObj withMeta(IPersistentMap newMeta) {
    return new PersistentHashMap(hb.shallowClone(newMeta));
  }
  public Object kvreduce(IFn f, Object init) {
    LeafNodeIterator iter = hb.iterator(hb.identityIterFn);
    while(iter.hasNext()) {
      LeafNode elem = iter.nextLeaf();
      init = f.invoke(init, elem.key(), elem.val());
      if (RT.isReduced(init))
	return ((IDeref)init).deref();
    }
    return init;
  }
  public TransientHashMap asTransient() {
    return new TransientHashMap(hb.shallowClone());
  }
}
