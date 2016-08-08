/*
 * Copyright 2016 Jin Kwon &lt;onacit_at_gmail.com&gt;.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jinahya.kt.ucloud.storage.client;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import static org.testng.Assert.fail;

/**
 *
 * @author Jin Kwon &lt;onacit_at_gmail.com&gt;
 */
public class JaxbTest {

    public static <T> void printXml(final Class<T> type, final T instance) {
        try {
            final JAXBContext context = JAXBContext.newInstance(type);
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(
                    Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(instance, System.out);
        } catch (final JAXBException jaxbe) {
            fail("failed to print xml", jaxbe);
        }
    }
}
