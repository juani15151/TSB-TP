package clases;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Clase para emular la funcionalidad de la clase java.util.Hashtable provista
 * en forma nativa por Java. Una TSBHashtable usa un arreglo de listas de la
 * clase TSBArrayList a modo de buckets (o listas de desborde) para resolver las
 * colisiones que pudieran presentarse.
 *
 * Se almacenan en la tabla pares de objetos (key, value), en donde el objeto
 * key actúa como clave para identificar al objeto value. La tabla no admite
 * repetición de claves (no se almacenarán dos pares de objetos con la misma
 * clave). Tampoco acepta referencias nulas (tanto para las key como para los
 * values): no será insertado un par (key, value) si alguno de ambos objetos es
 * null.
 *
 * Se ha emulado tanto como ha sido posible el comportamiento de la clase ya
 * indicada java.util.Hashtable. En esa clase, el parámetro loadFactor se usa
 * para determinar qué tan llena está la tabla antes de lanzar un proceso de
 * rehash: si loadFactor es 0.75, entonces se hará un rehash cuando la cantidad
 * de casillas ocupadas en el arreglo de soporte sea un 75% del tamaño de ese
 * arreglo. En nuestra clase TSBHashtable, mantuvimos el concepto de loadFactor
 * (ahora llamado load_factor) pero con una interpretación distinta: en nuestro
 * modelo, se lanza un rehash si la cantidad promedio de valores por lista es
 * mayor a cierto número constante y pequeño, que asociamos al load_factor para
 * mantener el espíritu de la implementación nativa. En nuestro caso, si el
 * valor load_factor es 0.8 entonces se lanzará un rehash si la cantidad
 * promedio de valores por lista es mayor a 0.8 * 10 = 8 elementos por lista.
 *
 * @author Ing. Valerio Frittelli.
 * @version Septiembre de 2017.
 * @param <K> el tipo de los objetos que serán usados como clave en la tabla.
 * @param <V> el tipo de los objetos que serán los valores de la tabla.
 */
public class TSBHashtable<K, V> implements Map<K, V>, Cloneable, Serializable {
    //************************ Constantes (privadas o públicas).    

    // el tamaño máximo que podrá tener el arreglo de soporte...
    // TODO: Modificar para que sea el mayor primo que admite un Integer.
    private final static int MAX_SIZE = Integer.MAX_VALUE;

    //************************ Atributos privados (estructurales).
    // la tabla hash: el arreglo que contiene las entradas...
    private Entry<K, V> table[];

    // el tamaño inicial de la tabla (tamaño con el que fue creada).
    private int initial_capacity;

    // la cantidad de objetos que contiene la tabla...
    private int count;

    // el factor de carga para calcular si hace falta un rehashing.
    // no debe ser mayor a 0.5f
    private float load_factor;

    //************************ Atributos privados (para gestionar las vistas).

    /*
     * (Tal cual están definidos en la clase java.util.Hashtable)
     * Cada uno de estos campos se inicializa para contener una instancia de la
     * vista que sea más apropiada, la primera vez que esa vista es requerida. 
     * La vista son objetos stateless (no se requiere que almacenen datos, sino 
     * que sólo soportan operaciones), y por lo tanto no es necesario crear más 
     * de una de cada una.
     */
    private transient Set<K> keySet = null;
    private transient Set<Map.Entry<K, V>> entrySet = null;
    private transient Collection<V> values = null;

    //************************ Atributos protegidos (control de iteración).
    // conteo de operaciones de cambio de tamaño (fail-fast iterator).
    protected transient int modCount;

    //************************ Constructores.
    /**
     * Crea una tabla vacía, con la capacidad inicial igual a 13 y con factor de
     * carga igual a 0.5f.
     */
    public TSBHashtable() {
        this(13, 0.5f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con factor de
     * carga igual a 0.5f.
     *
     * @param initial_capacity la capacidad inicial de la tabla.
     */
    public TSBHashtable(int initial_capacity) {
        this(initial_capacity, 0.5f);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con el factor
     * de carga indicado. La capacidad de la tabla debe ser un numero primo y el
     * factor de carga siempre menor a 0.5f para garantizar inserciones.
     *
     * @param initial_capacity la capacidad inicial de la tabla.
     * @param load_factor el factor de carga de la tabla.
     */
    public TSBHashtable(int initial_capacity, float load_factor) {
        if (initial_capacity <= 0) {
            initial_capacity = 13;
        } else if (initial_capacity > TSBHashtable.MAX_SIZE) {
            initial_capacity = TSBHashtable.MAX_SIZE;
        } else {
            initial_capacity = proximoPrimo(initial_capacity);
        }

        this.table = new Entry[initial_capacity];
        for (int i = 0; i < this.initial_capacity; i++) {
            table[i] = new Entry<>();
        }

        this.initial_capacity = initial_capacity;
        setLoadFactor(load_factor);
        this.count = 0;
        this.modCount = 0;
    }

    /**
     * Crea una tabla a partir del contenido del Map especificado.
     *
     * @param t el Map a partir del cual se creará la tabla.
     */
    public TSBHashtable(Map<? extends K, ? extends V> t) {
        // TODO: Modificar para crear las entradas inicializadas y no recorrer
        //       2 veces la tabla. (Al crear entry y al ponerles valor).
        this(13, 0.5f);
        this.putAll(t);
    }
    
    private int proximoPrimo(int initial){
        for(int i = initial; i < Integer.MAX_VALUE; i++){
            if(esPrimo(i)) return i;
        }
        throw new Error("No se encuentra un numero primo más grande.");
    }
    
    private boolean esPrimo(int num){
        if(num == 1) return false;
        if(num % 2 == 0) return false;
        
        for(int i = 3; i < Math.sqrt(num); i += 2){
            if(num % i == 0) return false;
        }
        return true;
    }
    
    private void setLoadFactor(float factor){
        if (factor <= 0 || factor > 0.5f) {
            this.load_factor = 0.5f;
        } else {
            this.load_factor = factor;
        }
    }
    
    //************************ Implementación de métodos especificados por Map.
    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     *
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Determina si la tabla está vacía (no contiene ningún elemento).
     *
     * @return true si la tabla está vacía.
     */
    @Override
    public boolean isEmpty() {
        return (this.count == 0);
    }

    /**
     * Determina si la clave key está en la tabla.
     *
     * @param key la clave a verificar.
     * @return true si la clave está en la tabla.
     * @throws NullPointerException si la clave es null.
     */
    @Override
    public boolean containsKey(Object key) {
        return (this.get((K) key) != null);
    }

    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a contains().
     *
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    @Override
    public boolean containsValue(Object value) {
        return this.contains(value);
    }

    /**
     * Retorna el objeto al cual está asociada la clave key en la tabla, o null
     * si la tabla no contiene ningún objeto asociado a esa clave.
     *
     * @param key la clave que será buscada en la tabla.
     * @return el objeto asociado a la clave especificada (si existe la clave) o
     * null (si no existe la clave en esta tabla).
     * @throws NullPointerException si key es null.
     * @throws ClassCastException si la clase de key no es compatible con la
     * tabla.
     */
    @Override
    public V get(Object key) {
        if (key == null) {
            throw new NullPointerException("get(): parámetro null");
        }

        Iterator<Integer> index = getIndexIterator(h(key.hashCode()));        
        Entry<K, V> entry;      

        while(index.hasNext()){
            entry = table[index.next()];
            EntryStatus s = entry.getStatus();
            if(s == EntryStatus.DISPONIBLE) return null;
            if(s == EntryStatus.OCUPADO){
                if(entry.getKey() == key) return entry.getValue();
            }
            // si el estado es tumba continua.
        }
        // se recorrieron todas las casillas y no esta el objeto.
        return null;        
    }
    
    /**
     * Asocia el valor (value) especificado, con la clave (key) especificada en
     * esta tabla. Si la tabla contenía previamente un valor asociado para la
     * clave, entonces el valor anterior es reemplazado por el nuevo (y en este
     * caso el tamaño de la tabla no cambia).
     *
     * @param key la clave del objeto que se quiere agregar a la tabla.
     * @param value el objeto que se quiere agregar a la tabla.
     * @return el objeto anteriormente asociado a la clave si la clave ya estaba
     * asociada con alguno, o null si la clave no estaba antes asociada a ningún
     * objeto.
     * @throws NullPointerException si key es null o value es null.
     */
    @Override
    public V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("put(): parámetro null");
        }
        if ((this.count + 1) / 2 >= load_factor) {
            rehash();
        }
        V old = null;
        Iterator<Integer> it = new CuadraticIterator(h(key.hashCode()), table.length);
        int i;
        while(it.hasNext()){
            i = it.next();
            if(table[i].status == EntryStatus.DISPONIBLE){                
                table[i] = new Entry(key, value);
                count++;
                modCount++;
                break;
            }
            if(table[i].key == key){
                if(table[i].status == EntryStatus.OCUPADO){
                    old = table[i].value;
                    table[i] = new Entry(key, value);
                    count++;
                    modCount++;
                    break;

                } else { // EntryStatus.tumba
                    table[i] = new Entry(key, value);
                    count++;
                    modCount++;
                    break;
                }    
            }            
        }     
        return old;
    }

    /**
     * Elimina de la tabla la clave key (y su correspondiente valor asociado).
     * El método no hace nada si la clave no está en la tabla.
     *
     * @param key la clave a eliminar.
     * @return El objeto al cual la clave estaba asociada, o null si la clave no
     * estaba en la tabla.
     * @throws NullPointerException - if the key is null.
     */
    @Override
    public V remove(Object key) {
        if (key == null) {
            throw new NullPointerException("remove(): parámetro null");
        }
        V old = null;
        Iterator<Integer> it = getIndexIterator(h(key.hashCode()));
        int i;
        while(it.hasNext()){
            i = it.next();
            if(table[i].status == EntryStatus.DISPONIBLE){                
                // El objeto no esta en la tabla.
                break;
            }
            if(table[i].key == key){
                if(table[i].status == EntryStatus.OCUPADO){
                    old = table[i].value;
                    table[i].setStatus(EntryStatus.TUMBA);
                    count--;
                    modCount++;
                    break;

                } else { // EntryStatus.tumba
                    // El objeto ya fue borrado.
                    break;
                }    
            }
        }
        
        return old;
    }

    /**
     * Copia en esta tabla, todos los objetos contenidos en el map especificado.
     * Los nuevos objetos reemplazarán a los que ya existan en la tabla
     * asociados a las mismas claves (si se repitiese alguna).
     *
     * @param m el map cuyos objetos serán copiados en esta tabla.
     * @throws NullPointerException si m es null.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.entrySet().forEach((e) -> {
            put(e.getKey(), e.getValue());
        });
    }

    /**
     * Elimina todo el contenido de la tabla, de forma de dejarla vacía. En esta
     * implementación además, el arreglo de soporte vuelve a tener el tamaño que
     * inicialmente tuvo al ser creado el objeto.
     */
    @Override
    public void clear() {
        this.table = new Entry[this.initial_capacity];
        for (int i = 0; i < this.table.length; i++) {
            this.table[i] = new Entry<>();
        }
        this.count = 0;
        this.modCount++;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todas las claves (key)
     * contenidas en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez eliminan
     * el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll() y
     * clear()). El conjunto vista no soporta las operaciones add() y addAll()
     * (si se las invoca, se lanzará una UnsuportedOperationException).
     *
     * @return un conjunto (un Set) a modo de vista de todas las claves mapeadas
     * en la tabla.
     */
    @Override
    public Set<K> keySet() {
        // TODO: Creo que no esta haciendo lo que dice. 
        //       (Como lo sincroniza con la tabla?)
        if (keySet == null) {
            // keySet = Collections.synchronizedSet(new KeySet()); 
            keySet = new KeySet();
        }
        return keySet;
    }

    /**
     * Retorna una Collection (colección) a modo de vista de todos los valores
     * (values) contenidos en la tabla. La colección está respaldada por la
     * tabla, por lo que los cambios realizados en la tabla serán reflejados en
     * la colección, y viceversa. Si la tabla es modificada mientras un iterador
     * está actuando sobre la colección vista, el resultado de la iteración será
     * indefinido (salvo que la modificación sea realizada por la operación
     * remove() propia del iterador, o por la operación setValue() realizada
     * sobre una entrada de la tabla que haya sido retornada por el iterador).
     * La colección vista provee métodos para eliminar elementos, y esos métodos
     * a su vez eliminan el correspondiente par (key, value) de la tabla (a
     * través de las operaciones Iterator.remove(), Collection.remove(),
     * removeAll(), removeAll(), retainAll() y clear()). La colección vista no
     * soporta las operaciones add() y addAll() (si se las invoca, se lanzará
     * una UnsuportedOperationException).
     *
     * @return una colección (un Collection) a modo de vista de todas los
     * valores mapeados en la tabla.
     */
    @Override
    public Collection<V> values() {
        // TODO: Creo que no esta haciendo lo que dice. 
        //       (Como lo sincroniza con la tabla?)
        if (values == null) {
            values = Collections.synchronizedCollection(new ValueCollection());
        //    values = new ValueCollection();
        }
        return values;
    }

    /**
     * Retorna un Set (conjunto) a modo de vista de todos los pares (key, value)
     * contenidos en la tabla. El conjunto está respaldado por la tabla, por lo
     * que los cambios realizados en la tabla serán reflejados en el conjunto, y
     * viceversa. Si la tabla es modificada mientras un iterador está actuando
     * sobre el conjunto vista, el resultado de la iteración será indefinido
     * (salvo que la modificación sea realizada por la operación remove() propia
     * del iterador, o por la operación setValue() realizada sobre una entrada
     * de la tabla que haya sido retornada por el iterador). El conjunto vista
     * provee métodos para eliminar elementos, y esos métodos a su vez eliminan
     * el correspondiente par (key, value) de la tabla (a través de las
     * operaciones Iterator.remove(), Set.remove(), removeAll(), retainAll() and
     * clear()). El conjunto vista no soporta las operaciones add() y addAll()
     * (si se las invoca, se lanzará una UnsuportedOperationException).
     *
     * @return un conjunto (un Set) a modo de vista de todos los objetos
     * mapeados en la tabla.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        // TODO: Creo que no esta haciendo lo que dice. 
        //       (Como lo sincroniza con la tabla?)
        if (entrySet == null) {
            entrySet = Collections.synchronizedSet(new EntrySet()); 
//            entrySet = new EntrySet();
        }
        return entrySet;
    }

    //************************ Redefinición de métodos heredados desde Object.
    /**
     * Retorna una copia superficial de la tabla. Las listas de desborde o
     * buckets que conforman la tabla se clonan ellas mismas, pero no se clonan
     * los objetos que esas listas contienen: en cada bucket de la tabla se
     * almacenan las direcciones de los mismos objetos que contiene la original.
     *
     * @return una copia superficial de la tabla.
     * @throws java.lang.CloneNotSupportedException si la clase no implementa la
     * interface Cloneable.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        TSBHashtable<K, V> copy = (TSBHashtable<K, V>) super.clone();
        copy.table = new Entry[table.length];
        for (int i = 0; i < table.length; i++) {            
            copy.table[i] = (Entry<K, V>) table[i].clone();
        }        
        copy.keySet = null;
        copy.entrySet = null;
        copy.values = null;
        copy.modCount = 0;
        return copy;
    }

    /**
     * Determina si esta tabla es igual al objeto espeficicado.
     *
     * @param obj el objeto a comparar con esta tabla.
     * @return true si los objetos son iguales.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Map)) {
            return false;
        }

        Map<K, V> t = (Map<K, V>) obj;
        if (t.size() != this.size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = this.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (t.get(key) == null) {
                    return false;
                } else if (!value.equals(t.get(key))) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }

        return true;
    }

    /**
     * Retorna un hash code para la tabla completa.
     *
     * @return un hash code para la tabla.
     */
    @Override
    public int hashCode() {
        if (this.isEmpty()) {
            return 0;
        }

        int hc = 0;
        hc = this.entrySet().stream().map((entry) -> entry.hashCode()).reduce(hc, Integer::sum);

        return hc;
    }

    /**
     * Devuelve el contenido de la tabla en forma de String. Sólo por razones de
     * didáctica, se hace referencia explícita en esa cadena al contenido de
     * cada una de las listas de desborde o buckets de la tabla.
     *
     * @return una cadena con el contenido completo de la tabla.
     */
    @Override
    public String toString() {
        StringBuilder cad = new StringBuilder("{ ");
        for (int i = 0; i < this.table.length; i++) {
            cad.append(i).append(" ");
        }
        cad.append('}');
        return cad.toString();
    }

    //************************ Métodos específicos de la clase.
    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a containsValue().
     *
     * @param value el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }
        
        V value;
        try {
            value = (V) obj;
        } catch (ClassCastException e){
            return false;
        }
        
        for(Entry<K,V> entry : this.table){
            if(entry.status == EntryStatus.OCUPADO &&
                    entry.getValue() == value){
                return true;
            }
        }
        return false;
    }
    
    public Entry<K, V> getEntry(K key) {
        if (key == null) {
            throw new NullPointerException("getEntry(): parámetro null");
        }

        Iterator<Integer> index = getIndexIterator(h(key.hashCode()));        
        Entry<K, V> entry;      

        while(index.hasNext()){
            entry = table[index.next()];
            EntryStatus s = entry.getStatus();
            if(s == EntryStatus.DISPONIBLE) return null;
            if(s == EntryStatus.OCUPADO){
                if(entry.getKey() == key) return entry;
            }
            // si el estado es tumba continua.
        }
        // se recorrieron todas las casillas y no esta el objeto.
        return null;        
    }

    /**
     * Incrementa el tamaño de la tabla y reorganiza su contenido. Se invoca
     * automaticamente cuando se detecta que la cantidad promedio de nodos por
     * lista supera a cierto el valor critico dado por (10 * load_factor). Si el
     * valor de load_factor es 0.8, esto implica que el límite antes de invocar
     * rehash es de 8 nodos por lista en promedio, aunque seria aceptable hasta
     * unos 10 nodos por lista.
     */
    protected void rehash() {
        int old_length = this.table.length;

        // nuevo tamaño: doble del anterior, más uno para llevarlo a impar...
        int new_length = proximoPrimo(old_length * 2 + 1);

        // no permitir que la tabla tenga un tamaño mayor al límite máximo...
        // ... para evitar overflow y/o desborde de índices...
        if (new_length > TSBHashtable.MAX_SIZE) {
            new_length = TSBHashtable.MAX_SIZE;
        }

        // crear el nuevo arreglo con new_length entradas...
        Entry<K, V> temp[] = new Entry[new_length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = new Entry();
        }

        // notificación fail-fast iterator... la tabla cambió su estructura...
        this.modCount++;

        // recorrer el viejo arreglo y redistribuir los objetos que tenia...
        for (Entry<K, V> x : this.table) {                       
            // obtener su nuevo valor de dispersión para el nuevo arreglo...
            K key = x.getKey();
            int y = this.h(key, temp.length);

            // insertarlo en el nuevo arreglo, en la lista numero "y"...
            temp[y] = new Entry(key, x.getValue());            
        }

        // cambiar la referencia table para que apunte a temp...
        this.table = temp;
    }

    //************************ Métodos privados.
    /*
     * Función hash. Toma una clave entera k y calcula y retorna un índice 
     * válido para esa clave para entrar en la tabla.     
     */
    private int h(int k) {
        return h(k, this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y calcula y 
     * retorna un índice válido para esa clave para entrar en la tabla.     
     */
    private int h(K key) {
        return h(key.hashCode(), this.table.length);
    }

    /*
     * Función hash. Toma un objeto key que representa una clave y un tamaño de 
     * tabla t, y calcula y retorna un índice válido para esa clave dedo ese
     * tamaño.     
     */
    private int h(K key, int t) {
        return h(key.hashCode(), t);
    }

    /*
     * Función hash. Toma una clave entera k y un tamaño de tabla t, y calcula y 
     * retorna un índice válido para esa clave dado ese tamaño.     
     */
    private int h(int k, int t) {
        if (k < 0) {
            k *= -1;
        }
        return k % t;
    }   
   
    private Iterator<Integer> getIndexIterator(int start){
        return new CuadraticIterator(start, table.length);
    }

    //************************ Clases Internas.
    
    /**
     * Iterador para recorrer los proximos indices de la tabla en forma cuadratica.
     */
    private class CuadraticIterator implements Iterator<Integer> {

        // El indice inicial.
        int start;
        
        int current_step;
        
        // La cantidad de iteraciones máxima.
        int max_step;
        
        public CuadraticIterator(int start, int max_step){
            this.start = start;
            this.current_step = 0;
            this.max_step = max_step;
        }
        
        @Override
        public boolean hasNext() {
            return current_step + 1 <= max_step;
        }

        @Override
        public Integer next() {
            if(current_step == 0){
                current_step++;
                return start;
            }
            return start + (int) Math.pow(current_step++, 2);
        }
        
    }
    
    /**
     * Iterador base para las vistas state-less.
     * Implementa Iterator<Entry<K,V>> pero no lo declara. (Renombra el metodo
     * next por nextEntry).
     */
    private abstract class EntryIterator {
        // índice de la lista actualmente recorrida...
            private int current_index;

            // índice de la lista anterior (si se requiere en remove())...
            private int last_index;

            // el valor que debería tener el modCount de la tabla completa...
            private int expected_modCount;

            /*
             * Crea un iterador comenzando en la primera lista. Activa el 
             * mecanismo fail-fast.
             */
            public EntryIterator() {
                current_index = this.buscarIndiceValido(-1);
                last_index = -1;
                expected_modCount = TSBHashtable.this.modCount;
            }

            /*
             * Determina si hay al menos un elemento en la tabla que no haya 
             * sido retornado por next(). 
             */
            public boolean hasNext() {
                return current_index != -1;
            }

            /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            public Entry<K,V> nextEntry() {
                // control: fail-fast iterator...
                if (TSBHashtable.this.modCount != expected_modCount) {
                    throw new ConcurrentModificationException("next(): modificación inesperada de tabla.");
                }

                if (!hasNext()) {
                    throw new NoSuchElementException("next(): no existe el elemento pedido.");
                }

                // variable auxiliar t para simplificar accesos...
                Entry<K, V> t[] = TSBHashtable.this.table;
                
                Entry<K, V> entry = t[current_index];
                last_index = current_index;
                // Calculo del proximo indice
                current_index = this.buscarIndiceValido(current_index);               
                return entry;
            }

            /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posición anterior al que fue removido. El elemento removido es el
             * que fue retornado la última vez que se invocó a next(). El método
             * sólo puede ser invocado una vez por cada invocación a next().
             */
            public void remove() {
                if (last_index == -1) {
                    throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");
                }

                // TODO: Creo que esto deberia ser un metodo de la HashTable,
                //       el iterador no deberia modificarla directamente.
                
                // eliminar el objeto que retornó next() la última vez...
                table[last_index].status = EntryStatus.TUMBA;
                last_index = -1;
                
                // la tabla tiene un elemento menos...
                TSBHashtable.this.count--;
                // fail_fast iterator: todo en orden...
                TSBHashtable.this.modCount++;
                expected_modCount++;
            }
            
            /**
             * Busca el primer indice válido por encima del pasado por parametro.
             * Se considera indice valido a aquel que apunta a un entry que esta
             * ocupado (no tumba ni disponible).
             * 
             * @param from el valor a partir del cual buscar (no lo incluye)
             * @return el proximo indice valido o -1 si no existe.
             */
            private int buscarIndiceValido(int from){
                for(int i = from + 1; i < table.length; i++){
                    if(table[i].status == EntryStatus.OCUPADO){
                        return i;
                    }
                }
                return -1;
            }
    }
    
    /*
     * Clase interna que representa los pares de objetos que se almacenan en la
     * tabla hash: son instancias de esta clase las que realmente se guardan en 
     * en cada una de las listas del arreglo table que se usa como soporte de 
     * la tabla. Lanzará una IllegalArgumentException si alguno de los dos 
     * parámetros es null.
     */
    private class Entry<K, V> implements Map.Entry<K, V> {
        
        private K key;
        private V value;

        // Los valores de cada Entry son: 
        // 0: Disponible
        // 1: Ocupado
        // 2: Tumba
        private EntryStatus status;
        
        public Entry(K key, V value) {
            if (key == null || value == null) {
                throw new IllegalArgumentException("Entry(): parámetro null...");
            }
            this.key = key;
            this.value = value;
            this.status = EntryStatus.OCUPADO;
        }

        private Entry() {
            this.key = null;
            this.value = null;
            this.status = EntryStatus.DISPONIBLE;
        }

        @Override
        public K getKey() {
            return key;
        }

        public EntryStatus getStatus() {
            return status;
        }
        
        private void setStatus(EntryStatus st) {
            this.status = st;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new IllegalArgumentException("setValue(): parámetro null...");
            }            
            V old = this.value;
            this.value = value;
            status = EntryStatus.OCUPADO;
            return old;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + Objects.hashCode(this.key);
            hash = 61 * hash + Objects.hashCode(this.value);
            return hash;
        }

        
        @Override
        protected Object clone() throws CloneNotSupportedException{        
            return new Entry(this.key, this.value);
        }
        
        
        @Override
        public boolean equals(Object obj) {
            if (this.status == EntryStatus.TUMBA) {
                
                return false;
            }
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }

            final Entry other = (Entry) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }

        @Override
        public String toString() {
            if (status == EntryStatus.OCUPADO) {
                return "(" + key.toString() + ", " + value.toString() + ")";
            }
            return "";
        }

        public String toStringWithDead() {
            if (status == EntryStatus.OCUPADO) {
                return "(" + key.toString() + ", " + value.toString() + ", alive)";
            }
            if (status == EntryStatus.TUMBA) {
                return "(" + key.toString() + ", " + value.toString() + ", dead)";
            }
            return "";
        }
        
    }

    /*
     * Clase interna que representa una vista de todas los Claves mapeadas en la
     * tabla: si la vista cambia, cambia también la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no 
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */
    private class KeySet extends AbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return new KeySetIterator();
        }

        @Override
        public int size() {
            return TSBHashtable.this.count;
        }

        @Override
        public boolean contains(Object o) {
            return TSBHashtable.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return (TSBHashtable.this.remove(o) != null);
        }

        @Override
        public void clear() {
            TSBHashtable.this.clear();
        }

        private class KeySetIterator extends TSBHashtable.EntryIterator implements Iterator<K> {

             /*
             * Retorna el siguiente elemento disponible en la tabla.
             */
            @Override
            public K next() {
                // TODO: No se porque obliga a castear, getKey siempre retorna K...
                return (K) super.nextEntry().getKey();
            }            
        }
    }

    /*
     * Clase interna que representa una vista de todos los PARES mapeados en la
     * tabla: si la vista cambia, cambia también la tabla que le da respaldo, y
     * viceversa. La vista es stateless: no mantiene estado alguno (es decir, no 
     * contiene datos ella misma, sino que accede y gestiona directamente datos
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */
    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntrySetIterator();
        }

        /*
         * Verifica si esta vista (y por lo tanto la tabla) contiene al par 
         * que entra como parámetro (que debe ser de la clase Entry).
         */
        @Override
        public boolean contains(Object o) {
            if (o == null) {
                return false;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
            K key = entry.getKey();
            
            return TSBHashtable.this.getEntry(key) == entry;                        
        }

        /*
         * Elimina de esta vista (y por lo tanto de la tabla) al par que entra
         * como parámetro (y que debe ser de tipo Entry).
         */
        @Override
        public boolean remove(Object o) {
            if (o == null) {
                throw new NullPointerException("remove(): parámetro null");
            }
            if (!(o instanceof Entry)) {
                return false;
            }          
            Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
            K key = entry.getKey();
            
            // TODO: No estoy seguro, creo que deberia devolver true si el entry
            //       no existe.
            return TSBHashtable.this.remove(entry.getKey()) != null;
        }

        @Override
        public int size() {
            return TSBHashtable.this.count;
        }

        @Override
        public void clear() {
            TSBHashtable.this.clear();
        }

        private class EntrySetIterator extends TSBHashtable.EntryIterator implements Iterator<Map.Entry<K, V>> {

            @Override
            public Entry<K, V> next() {
                return super.nextEntry();
            }            
        }
    }

    /*
     * Clase interna que representa una vista de todos los VALORES mapeados en 
     * la tabla: si la vista cambia, cambia también la tabla que le da respaldo, 
     * y viceversa. La vista es stateless: no mantiene estado alguno (es decir, 
     * no contiene datos ella misma, sino que accede y gestiona directamente los
     * de otra fuente), por lo que no tiene atributos y sus métodos gestionan en
     * forma directa el contenido de la tabla. Están soportados los metodos para
     * eliminar un objeto (remove()), eliminar todo el contenido (clear) y la  
     * creación de un Iterator (que incluye el método Iterator.remove()).
     */
    private class ValueCollection extends AbstractCollection<V> {

        @Override
        public Iterator<V> iterator() {
            return new ValueCollectionIterator();
        }

        @Override
        public int size() {
            return TSBHashtable.this.count;
        }

        @Override
        public boolean contains(Object o) {
            return TSBHashtable.this.containsValue(o);
        }

        @Override
        public void clear() {
            TSBHashtable.this.clear();
        }      

        private class ValueCollectionIterator extends TSBHashtable.EntryIterator implements Iterator<V> {

            @Override
            public V next() {
                // TODO: No entiendo porque obliga a castear a V, es el unico 
                //       objeto que retorna getValue().
                return (V) super.nextEntry().getValue();
            }            
        }
    }
}