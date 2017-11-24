/**
 * Copyright gradecak.com

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gradecak.alfresco.querytemplate;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.QName;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Persistable;
import org.springframework.util.Assert;

public abstract class BeanPropertiesMapper<T extends Persistable<NodeRef>> implements NodePropertiesMapper<T> {

  protected final ServiceRegistry serviceRegistry;
  protected Class<T> mappedClass;
  protected Map<String, PropertyDescriptor> mappedFields;
  protected Map<QName, PropertyDescriptor> mappedQNames;
  protected Set<String> mappedProperties;

  @SuppressWarnings("unchecked")
  public BeanPropertiesMapper(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
    Class<T> mappedClass = (Class<T>)GenericTypeResolver.resolveTypeArgument(this.getClass(), NodePropertiesMapper.class);
    setMappedClass(mappedClass);
  }
  
  public BeanPropertiesMapper(final ServiceRegistry serviceRegistry, Class<T> mappedClass) {
    this.serviceRegistry = serviceRegistry;
    setMappedClass(mappedClass);
  }

  public T mapNodeProperties(final NodeRef nodeRef, final Map<QName, Serializable> properties) {
    Assert.state(this.mappedClass != null, "Mapped class was not specified");
    T mappedObject = BeanUtils.instantiate(this.mappedClass);
    BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);
    initBeanWrapper(bw);

    for (Map.Entry<QName, PropertyDescriptor> entry : mappedQNames.entrySet()) {
      QName qName = entry.getKey();

      PropertyDescriptor pd = entry.getValue();
      if (pd != null) {
        bw.setPropertyValue(pd.getName(), properties.get(qName));
      }
    }
   
    if(bw.isWritableProperty("id")) {
      PropertyDescriptor idProperty = bw.getPropertyDescriptor("id");
      if (idProperty != null) {
        bw.setPropertyValue(idProperty.getName(), nodeRef);
      }
    }

    configureMappedObject(mappedObject);

    return mappedObject;
  }

  protected void configureMappedObject(T mappedObject) {}

  /**
   * Initialize the given BeanWrapper to be used for row mapping. To be called for each row.
   * <p>
   * The default implementation is empty. Can be overridden in subclasses.
   * 
   * @param bw the BeanWrapper to initialize
   */
  protected void initBeanWrapper(BeanWrapper bw) {}

  /**
   * Set the class that properties should be mapped to.
   */
  protected void setMappedClass(Class<T> mappedClass) {
    if (this.mappedClass == null) {
      initialize(mappedClass);
    } else {
      if (!this.mappedClass.equals(mappedClass)) {
        throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to " + mappedClass + " since it is already providing mapping for " + this.mappedClass);
      }
    }
  }

  /**
   * Initialize the mapping metadata for the given class.
   * 
   * @param mappedClass the mapped class.
   */
  private void initialize(Class<T> mappedClass) {
    this.mappedClass = mappedClass;
    this.mappedFields = new HashMap<String, PropertyDescriptor>();
    this.mappedProperties = new HashSet<String>();
    this.mappedQNames = new HashMap<QName, PropertyDescriptor>();
    PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
    for (PropertyDescriptor pd : pds) {
      if (pd.getWriteMethod() != null) {
        this.mappedFields.put(pd.getName().toLowerCase(), pd);
        String prefixName = prefixName(pd.getName());
        if (!pd.getName().toLowerCase().equals(prefixName)) {
          this.mappedFields.put(prefixName, pd);
        }
        this.mappedProperties.add(pd.getName());

        String prefixedString = prefixName.replaceFirst("_", ":");

        if (prefixedString.contains(":")) {
          try {
            QName qName = QName.createQName(prefixedString, serviceRegistry.getNamespaceService());
            if (serviceRegistry.getDictionaryService().getProperty(qName) != null) {
              this.mappedQNames.put(qName, pd);
            }
          } catch (NamespaceException e) {
            // ; noop
          }
        }
      }
    }
  }

  /**
   * Convert a name in camelCase to an underscored name in lower case. Any upper case letters are converted to lower
   * case with a preceding underscore.
   * 
   * @param name the string containing original name
   * @return the converted name
   */
  private String prefixName(String name) {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    if (name != null && name.length() > 0) {
      result.append(name.substring(0, 1).toLowerCase());
      for (int i = 1; i < name.length(); i++) {
        String s = name.substring(i, i + 1);
        if (s.equals(s.toUpperCase())) {
          if (first) {
            result.append("_");
            result.append(s.toLowerCase());
            first = false;
          } else {
            result.append(s);
          }
        } else {
          result.append(s);
        }
      }
    }
    return result.toString();
  }
  
  public ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }
}