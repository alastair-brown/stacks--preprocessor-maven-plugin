package com.ensono.stacks.utils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class XmlUtils {

    public static Iterable<Node> iterable(final NodeList nodeList) {
        return () -> new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                return nodeList.item(index++);
            }
        };
    }

}
