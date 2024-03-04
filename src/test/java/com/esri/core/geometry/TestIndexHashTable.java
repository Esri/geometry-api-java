package com.esri.core.geometry;
import org.junit.Test;
import junit.framework.TestCase;
public class TestIndexHashTable extends TestCase{
    @Test
    public void testAddElement() {
        IndexHashTable.HashFunction hashFunction = new IndexHashTable.HashFunction() {
            @Override
            public int getHash(int element) {
                return element % 10; // A simple hash function for testing
            }

            @Override
            public boolean equal(int element1, int element2) {
                return element1 == element2;
            }

            @Override
            public int getHash(Object elementDescriptor) {
                return ((Integer) elementDescriptor) % 10;
            }

            @Override
            public boolean equal(Object elementDescriptor, int element) {
                return ((Integer) elementDescriptor) == element;
            }
        };

        IndexHashTable hashTable = new IndexHashTable(10, hashFunction);

        int element1 = 5;

        int node1 = hashTable.addElement(element1);

        assertEquals(node1, hashTable.findNode(element1));
    }

    @Test
    public void testDeleteElement() {
        IndexHashTable.HashFunction hashFunction = new IndexHashTable.HashFunction() {
            @Override
            public int getHash(int element) {
                return element % 10; // A simple hash function for testing
            }

            @Override
            public boolean equal(int element1, int element2) {
                return element1 == element2;
            }

            @Override
            public int getHash(Object elementDescriptor) {
                return ((Integer) elementDescriptor) % 10;
            }

            @Override
            public boolean equal(Object elementDescriptor, int element) {
                return ((Integer) elementDescriptor) == element;
            }
        };

        IndexHashTable hashTable = new IndexHashTable(10, hashFunction);

        int element1 = 5;

        int node1 = hashTable.addElement(element1);

        hashTable.deleteElement(element1);
        assertEquals(IndexHashTable.nullNode(), hashTable.findNode(element1));
    }

}
