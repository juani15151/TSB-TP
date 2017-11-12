package clases;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Clase que provee una tabla Hash con direccionamiento abierto.
 *
 * @version Noviembre de 2017.
 * @param <K> el tipo de los objetos que serán usados como clave en la tabla.
 * @param <V> el tipo de los objetos que serán los valores de la tabla.
 */
public class TSBHashtable<K, V> implements Map<K, V>, Cloneable, Serializable {
    //************************ Constantes.    

    // el tamaño máximo que podrá tener el arreglo de soporte...
    // TODO: Modificar para que sea el mayor primo que admite un Integer.
    private final static int MAX_CAPACITY = Integer.MAX_VALUE;
    
    // Capacidad por defecto.
    private final static int DEFAULT_CAPACITY = 10;
    
    // Factor de carga por defecto. NO debe ser mayor a 0.5f
    private final static float DEFAULT_LOAD_FACTOR = 0.5f;

    //************************ Atributos privados (estructurales).
    // la tabla hash: el arreglo que contiene las entradas...
    private Entry<K, V> table[];

    // el tamaño inicial de la tabla (tamaño con el que fue creada). Corresponde
    // al primer tamaño válido (nro primo) mayor al requerido.
    private int initialCapacity;

    // la cantidad de objetos que contiene la tabla.
    private int size;

    // el factor de carga para calcular si hace falta un rehashing.
    // no debe ser mayor a 0.5f para asegurar que el direccionamiento abierto
    // funcione.
    private float loadFactor;

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
    public TSBHashtable() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con factor de
     * carga igual a 0.5f.
     *
     * @param initial_capacity la capacidad inicial de la tabla.
     */
    public TSBHashtable(int initial_capacity) {
        this(initial_capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Crea una tabla vacía, con la capacidad inicial indicada y con el factor
     * de carga indicado. La capacidad de la tabla debe ser un numero primo y el
     * factor de carga siempre menor a 0.5f para garantizar inserciones.
     *
     * @param initial_capacity la capacidad inicial usable de la tabla.
     * @param load_factor el factor de carga de la tabla.
     */
    public TSBHashtable(int initial_capacity, float load_factor) {
        setLoadFactor(load_factor);
        setInitialCapacity(initial_capacity); // Debe estar despues de setLoadFactor();
        this.table = new Entry[initial_capacity];      
        this.size = 0;
        this.modCount = 0;
    }

    /**
     * Crea una tabla a partir del contenido del Map especificado.
     *
     * @param t el Map a partir del cual se creará la tabla.
     */
    public TSBHashtable(Map<? extends K, ? extends V> t) {
        // Crea un arreglo con el tamaño necesario para contener el mapa y poder
        // agregar nuevos elementos si hacer un rehash en el primer intento.
        this(t.size() * 2 + 1, 0.5f);
        this.putAll(t);
    }
    
    private void setLoadFactor(float factor) {
        loadFactor = factor < 0 || factor > 0.5f ? DEFAULT_LOAD_FACTOR : factor;
    }
    
    /**
     * Define el tamaño del arreglo minimo para almacenar la capacidad 
     * solicitada. 
     * Antes de invocarlo debe estar definido load_factor.
     * @param initial_capacity 
     */
    private void setInitialCapacity(int initial_capacity) {
        if (initial_capacity <= 0) {
            initial_capacity = DEFAULT_CAPACITY;
        } else if (initial_capacity > TSBHashtable.MAX_CAPACITY) {
            initial_capacity = TSBHashtable.MAX_CAPACITY;
        } else {
            /* 
             * La capacidad inicial se aumenta en funcion del load_factor de 
             * forma que la tabla en su maxima capacidad pueda contener la 
             * cantidad de elementos pasada por parametro.
             */
            initial_capacity = (int) ((float) initial_capacity / loadFactor);
            initial_capacity = proximoPrimo(initial_capacity);
        }
        this.initialCapacity = initial_capacity;
    }

    private int proximoPrimo(int initial) {
        for (int i = initial; i < Integer.MAX_VALUE; i++) {
            if (esPrimo(i)) {
                return i;
            }
        }
        throw new Error("No se encuentra un numero primo más grande.");
    }

    private boolean esPrimo(int num) {
        // Si es 1 o par
        if (num == 1 || num % 2 == 0) {
            return false;
        }
        // Busqueda de divisores.
        final int raiz = (int) Math.sqrt(num) + 1; // Se suma 1 por el redondeo.
        for (int i = 3; i < raiz; i += 2) {
            if (num % i == 0) {
                return false;
            }
        }
        return true;
    }   

    // ***** Implementación de métodos especificados por Map. *****
    /**
     * Retorna la cantidad de elementos contenidos en la tabla.
     *
     * @return la cantidad de elementos de la tabla.
     */
    @Override
    public int size() {
        return this.size;
    }

    /**
     * Determina si la tabla está vacía (no contiene ningún elemento).
     *
     * @return true si la tabla está vacía.
     */
    @Override
    public boolean isEmpty() {
        return this.size == 0;
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
        return this.get((K) key) != null;
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
    public V get(Object object) {
        if (object == null) {
            throw new NullPointerException("get(): parámetro null");
        }
        K key = (K) object; // throws ClassCastException si object no es un Key.
        Entry<K, V> e = getEntry(key);
        return e != null ? e.getValue() : null;
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
        if ((size + 1) / table.length >= loadFactor) {
            rehash();
        }
        // TODO: Todo este codigo es muy similar a getEntry, si se los puede
        //       unificar de alguna manera, se reduce el codigo duplicado.
        V old = null;
        Iterator<Integer> it = getIndexIterator(h(key.hashCode()));
        int i;
        while (it.hasNext()) {
            i = it.next();
            if (table[i] == null) {
                table[i] = new Entry(key, value);
                break;
            }
            else if (table[i].getKey().equals(key)) {
                // Si esta ocupado retorna el objeto, si es tumba retorna null.
                old = table[i].setValue(value);
                break;
            }
        }
        if (old == null) {
            size++;
        }
        modCount++;
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
        // throws ¿ClassCastException? o similar si key no es un K valido.
        Entry<K, V> entry = this.getEntry((K) key);
        
        if (entry != null && entry.alive()){
            size--;
            modCount++;
            return entry.kill();
        }
        return null;
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
        this.table = new Entry[initialCapacity];
        this.size = 0;
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
        if (keySet == null) {
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
        if (values == null) {
            values = new ValueCollection();
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
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
        return entrySet;
    }

    //************************ Redefinición de métodos heredados desde Object.
    /**
     * Retorna una copia superficial de la tabla. Las entradas que conforman la
     * tabla se clonan, pero no se clonan los objetos que estas contienen: en
     * cada entrada de la tabla se almacenan las direcciones de los mismos
     * objetos que contiene la original.
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

        Map<K, V> other = (Map<K, V>) obj;
        if (other.size() != this.size()) {
            return false;
        }

        try {
            Iterator<Map.Entry<K, V>> i = this.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<K, V> thisEntry = i.next();
                V otherValue = other.get(thisEntry.getKey());
                if (otherValue == null
                        || !thisEntry.getValue().equals(otherValue)) {
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
        StringBuilder cad = new StringBuilder("HashTable: ");
        cad.append("initialCap:").append(initialCapacity);
        cad.append("; count:").append(this.size);
        cad.append("; {");

        // Recorrido lineal de la tabla
        for (Entry<K, V> entry : table) {
            if (entry != null && entry.alive()) {
                cad.append(entry.toString()).append(" ");
            }
        }
        cad.append('}');
        return cad.toString();
    }

    //************************ Métodos específicos de la clase.
    /**
     * Determina si alguna clave de la tabla está asociada al objeto value que
     * entra como parámetro. Equivale a containsValue().
     *
     * @param obj el objeto a buscar en la tabla.
     * @return true si alguna clave está asociada efectivamente a ese value.
     */
    public boolean contains(Object obj) {
        if (obj == null) {
            return false;
        }

        V value;
        try {
            value = (V) obj;
        } catch (ClassCastException e) {
            return false;
        }

        // Recorrido lineal de las entradas porque se desconoce la clave.
        for (Entry<K, V> entry : this.table) {
            if (entry != null
                    && entry.alive()
                    && entry.getValue().equals(value)) {

                return true;
            }
        }
        return false;
    }
    
    /**
     * Retorna la entrada de la clave indicada o null si no existe.
     * Filtra las entradas muertas.
     * 
     * @param key
     * @return la entrada buscada o null si no existe.
     */
    private Entry<K, V> getEntry(K key) {
        if (key == null) {
            throw new NullPointerException("getEntry(): parámetro null");
        }

        Iterator<Integer> index = getIndexIterator(h(key.hashCode()));
        Entry<K, V> entry;

        while (index.hasNext()) {
            entry = table[index.next()];
            if (entry == null) {
                return null;
            }
            if(entry.getKey().equals(key)){
                // Si la entrada es tumba y tiene esta clave retorna null.
                return entry.alive() ? entry : null;
            }
        }
        // se recorrieron todas las casillas y no esta el objeto.
        return null;
    }

    /**
     * Incrementa el tamaño de la tabla y reorganiza su contenido. Se invoca
     * automaticamente cuando se detecta que la cantidad de objetos supera el 
     * factor de carga establecido.
     */
    protected void rehash() {
        int old_length = table.length;

        // nuevo tamaño: doble del anterior, más uno para llevarlo a impar...
        int new_length = proximoPrimo(old_length * 2 + 1);

        // no permitir que la tabla tenga un tamaño mayor al límite máximo...
        // ... para evitar overflow y/o desborde de índices...
        if (new_length > TSBHashtable.MAX_CAPACITY) {
            new_length = TSBHashtable.MAX_CAPACITY;
        }

        // crear el nuevo arreglo con new_length entradas...
        Entry<K, V> new_table[] = new Entry[new_length];

        // notificación fail-fast iterator... la tabla cambió su estructura...
        this.modCount++;

        // recorrer el viejo arreglo y redistribuir los objetos que tenia...
        for (Entry<K, V> x : this.table) {
            if (x == null) {
                continue;
            }
            // obtener su nuevo valor de dispersión para el nuevo arreglo...
            K key = x.getKey();
            int y = this.h(key, new_table.length);

            // insertarlo en el nuevo arreglo, en la lista numero "y"...
            new_table[y] = new Entry(key, x.getValue());
        }

        // cambiar la referencia table para que apunte a temp...
        this.table = new_table;
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

    private Iterator<Integer> getIndexIterator(int start) {
        // Puesto en un metodo para reducir dependencia, el iterador se puede
        // cambiar por otro (ej. lineal) sin mayores inconvenientes.
        return new CuadraticIndexIterator(start, table.length);
    }

    //************************ Clases Internas.
    /**
     * Iterador para recorrer los proximos indices de la tabla en forma
     * cuadratica.
     * El iterador garantiza que recorrera todas las entradas de la tabla antes
     * de repetir un elemento si se cumple que el tamaño del arreglo es un 
     * numero primo y la carga de la tabla menor al 50%.
     */
    private class CuadraticIndexIterator implements Iterator<Integer> {

        // El indice inicial. (lo devolvera el primer next() e ira aumentando 
        // segun una funcion cuadratica)
        int start;

        // Cantidad de invocaciones a next() (iteraciones del iterador).
        int iterations;

        // El numero maximo (exclusivo) que retornara el iterador.
        int length;

        public CuadraticIndexIterator(int startIndex, int arrayLength) {
            this.start = startIndex;
            this.iterations = 0;
            this.length = arrayLength;
        }

        @Override
        public boolean hasNext() {
            return iterations < length;
        }

        @Override
        public Integer next() {
            // La primera vez retorna el mismo índice (+ 0)
            if (iterations == 0) {
                iterations++;
                return start;
            }
            int index = start + (int) Math.pow(iterations++, 2);
            // TODO: Si se hacen muchas invocaciones el arreglo crece mucho,
            //       restar de a 1 no es muy optimo...
            while (index >= length){
                index -= length;
            }
            return index;
        }

    }

    /**
     * Iterador base para las vistas state-less. Implementa Iterator<Entry<K,V>>
     * pero no lo declara. (Renombra el metodo next por nextEntry).
     */
    private abstract class EntryIterator {

        // índice de la lista actualmente recorrida...
        private int currentIndex;

        // índice de la lista anterior (si se requiere en remove())...
        private int lastIndex;

        // el valor que debería tener el modCount de la tabla completa...
        private int expectedModCount;

        /*
             * Crea un iterador comenzando en la primera lista. Activa el 
             * mecanismo fail-fast.
         */
        public EntryIterator() {
            currentIndex = this.buscarIndiceValido(-1);
            lastIndex = -1;
            expectedModCount = TSBHashtable.this.modCount;
        }

        /*
             * Determina si hay al menos un elemento en la tabla que no haya 
             * sido retornado por next(). 
         */
        public boolean hasNext() {
            return currentIndex != -1;
        }

        /*
             * Retorna el siguiente elemento disponible en la tabla.
         */
        public Entry<K, V> nextEntry() {
            // control: fail-fast iterator...
            if (TSBHashtable.this.modCount != expectedModCount) {
                throw new ConcurrentModificationException("next(): modificación inesperada de tabla.");
            }

            if (!hasNext()) {
                throw new NoSuchElementException("next(): no existe el elemento pedido.");
            }

            // variable auxiliar t para simplificar accesos...
            Entry<K, V> t[] = TSBHashtable.this.table;

            Entry<K, V> entry = t[currentIndex];
            lastIndex = currentIndex;
            // Calculo del proximo indice
            currentIndex = this.buscarIndiceValido(currentIndex);
            return entry;
        }

        /*
             * Remueve el elemento actual de la tabla, dejando el iterador en la
             * posición anterior al que fue removido. El elemento removido es el
             * que fue retornado la última vez que se invocó a next(). El método
             * sólo puede ser invocado una vez por cada invocación a next().
         */
        public void remove() {
            if (lastIndex == -1) {
                throw new IllegalStateException("remove(): debe invocar a next() antes de remove()...");
            }

            // TODO: Creo que esto deberia ser un metodo de la HashTable,
            //       el iterador no deberia modificarla directamente.
            // eliminar el objeto que retornó next() la última vez...
            table[lastIndex].kill();
            lastIndex = -1;

            // la tabla tiene un elemento menos...
            TSBHashtable.this.size--;
            // fail_fast iterator: todo en orden...
            TSBHashtable.this.modCount++;
            expectedModCount++;
        }

        /**
         * Busca el primer indice válido por encima del pasado por parametro. Se
         * considera indice valido a aquel que apunta a un entry que esta
         * ocupado (no tumba ni disponible).
         *
         * @param from el valor a partir del cual buscar (no lo incluye)
         * @return el proximo indice valido o -1 si no existe.
         */
        private int buscarIndiceValido(int from) {
            for (int i = from + 1; i < table.length; i++) {
                if (table[i] != null && table[i].alive()) {
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
    private class Entry<K, V> implements Map.Entry<K, V>, Serializable {

        private K key;
        private V value;

        // Estado de la entrada. (Viva/Muerta)
        private boolean alive;

        public Entry(K key, V value) {
            if (key == null || value == null) {
                throw new IllegalArgumentException("Entry(): parámetro null...");
            }
            this.key = key;
            this.value = value;
            this.alive = true;
        }

        public boolean alive() {
            return alive;
        }

        public boolean dead() {
            return !alive;
        }

        public V kill() {
            V old = value;
            value = null;
            alive = false;
            return old;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        /**
         * Cambia el valor almacenado y pasa a estado ocupado.
         *
         * @returns el valor almacenado.
         */
        public V setValue(V newValue) {
            if (newValue == null) {
                throw new IllegalArgumentException("setValue(): parámetro null...");
            }
            V old = this.value;
            this.value = newValue;
            alive = true;
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
        protected Object clone() throws CloneNotSupportedException {
            return new Entry(this.key, this.value);
        }

        @Override
        public boolean equals(Object obj) {
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
            if(other.alive != this.alive){
                return false;
            }            
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return Objects.equals(this.value, other.value);
        }

        @Override
        /**
         * Muestra su contenido solo si esta ocupado (no muerto).
         */
        public String toString() {
            if (alive) {
                return "(" + key.toString() + ", " + value.toString() + ")";
            }
            return "";
        }

        public String toStringWithDead() {
            if (alive) {
                return "(" + key.toString() + ", " + value.toString() + ", alive)";
            } else {            
                return "(" + key.toString() + ", " + value.toString() + ", dead)";
            }
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
            return TSBHashtable.this.size;
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
                // Hay que castear porque tengo acceso a tablas con distinto
                // K (no es necesariamente el mismo).
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
    private class EntrySet extends AbstractSet<Map.Entry<K, V>>{

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

            // TODO: No estoy seguro, tal vez deberia devolver true si el entry
            //       no existe.
            return TSBHashtable.this.remove(entry.getKey()) != null;
        }

        @Override
        public int size() {
            return TSBHashtable.this.size();
        }

        @Override
        public void clear() {
            TSBHashtable.this.clear();
        }

        private class EntrySetIterator extends TSBHashtable.EntryIterator 
                implements Iterator<Map.Entry<K, V>> {

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
            return TSBHashtable.this.size();
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
                return (V) super.nextEntry().getValue();
            }
        }
    }
}
