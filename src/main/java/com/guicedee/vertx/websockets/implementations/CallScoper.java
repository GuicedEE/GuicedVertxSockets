package com.guicedee.vertx.websockets.implementations;

import com.google.common.collect.Maps;
import com.google.inject.*;
import com.guicedee.client.*;
import com.guicedee.guicedservlets.servlets.services.IOnCallScopeEnter;
import com.guicedee.guicedservlets.servlets.services.IOnCallScopeExit;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkState;


public class CallScoper implements Scope
{
	private static final Provider<Object> SEEDED_KEY_PROVIDER =
					new Provider<Object>()
					{
						public Object get()
						{
							throw new IllegalStateException("If you got here then it means that" +
											" your code asked for scoped object which should have been" +
											" explicitly seeded in this scope by calling" +
											" CallScoper.seed(), but was not.");
						}
					};
	private static final ThreadLocal<Map<Key<?>, Object>> values
					= new ThreadLocal<Map<Key<?>, Object>>();
	
	private boolean startedScope;
	
	public void enter()
	{
		try
		{
			checkState(values.get() == null, "A scoping block is already in progress");
			values.set(Maps.<Key<?>, Object>newHashMap());
			startedScope = true;
			@SuppressWarnings("rawtypes")
			Set<IOnCallScopeEnter> scopeEnters = IGuiceContext.loaderToSet(ServiceLoader.load(IOnCallScopeEnter.class));
			for (IOnCallScopeEnter<?> scopeEnter : scopeEnters)
			{
				try
				{
					scopeEnter.onScopeEnter(this);
				} catch (Throwable T)
				{
					Logger.getLogger("CallScoper")
									.log(Level.WARNING, "Exception on scope entry - " + scopeEnter, T);
				}
			}
		} catch (Throwable T)
		{
			startedScope = false;
		}
	}
	
	public void exit()
	{
		try
		{
			checkState(values.get() != null, "No scoping block in progress");
			Set<IOnCallScopeExit> scopeEnters = IGuiceContext.loaderToSet(ServiceLoader.load(IOnCallScopeExit.class));
			for (IOnCallScopeExit<?> scopeEnter : scopeEnters)
			{
				try
				{
					scopeEnter.onScopeExit();
				} catch (Throwable T)
				{
					Logger.getLogger("CallScoper")
									.log(Level.WARNING, "Exception on call scope exit", T);
				}
			}
			values.remove();
		} catch (IllegalStateException T)
		{
			Logger.getLogger("CallScoper")
							.log(Level.WARNING, "NOT IN SCOPE ", T);
			
		} catch (Throwable T)
		{
			Logger.getLogger("CallScoper")
							.log(Level.WARNING, "Cannot perform close scope", T);
		}
	}
	
	public <T> void seed(Key<T> key, T value)
	{
		Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
		checkState(!scopedObjects.containsKey(key), "A value for the key %s was " +
										"already seeded in this scope. Old value: %s New value: %s", key,
						scopedObjects.get(key), value);
		scopedObjects.put(key, value);
	}
	
	public <T> void seed(Class<T> clazz, T value)
	{
		seed(Key.get(clazz), value);
	}
	
	public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped)
	{
		return new Provider<T>()
		{
			public T get()
			{
				Map<Key<?>, Object> scopedObjects = getScopedObjectMap(key);
				
				@SuppressWarnings("unchecked")
				T current = (T) scopedObjects.get(key);
				if (current == null && !scopedObjects.containsKey(key))
				{
					current = unscoped.get();
					
					// don't remember proxies; these exist only to serve circular dependencies
					if (Scopes.isCircularProxy(current))
					{
						return current;
					}
					
					scopedObjects.put(key, current);
				}
				return current;
			}
		};
	}
	
	private <T> Map<Key<?>, Object> getScopedObjectMap(Key<T> key)
	{
		Map<Key<?>, Object> scopedObjects = values.get();
		if (scopedObjects == null)
		{
			//enter();
		//	scopedObjects = values.get();
			throw new OutOfScopeException("Cannot access " + key
			                              + " outside of a scoping block - " + key.toString());
		}
		return scopedObjects;
	}
	
	/**
	 * Returns a provider that always throws exception complaining that the object
	 * in question must be seeded before it can be injected.
	 *
	 * @return typed provider
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> Provider<T> seededKeyProvider()
	{
		return (Provider<T>) SEEDED_KEY_PROVIDER;
	}
	
}
