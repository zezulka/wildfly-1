package org.jboss.as.test.integration.ejb.async;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;

/**
 * Async stateless bean that has parent.
 *  
 * @author Ondrej Chaloupka
 */
@Stateless
@Asynchronous
public class AsyncBeanInherited extends AsyncBeanParent implements AsyncBeanRemoteInterface {
    // parent class
}