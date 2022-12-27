package com.github.kidplayer.proxy;/*
 *Copyright © 2022 SMLOG
 *SMLOG
 *https://smlog.github.io
 *All rights reserved.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class MemCacheManager {



	private static LinkedHashMap<String,IndexCache> indexCacheMap =new LinkedHashMap<String,IndexCache>();

	//private static BlockingQueue queue = new LinkedBlockingDeque<IndexCache>() ;

	public static    CacheItem  curTsUrl(String url) {

		IndexCache cache;

		Iterator<String> it = indexCacheMap.keySet().iterator();
		CacheItem cacheItem = null;
		while(it.hasNext()) {
			String key = it.next();
			synchronized(indexCacheMap){
				cache = indexCacheMap.get(key);
			}
			int index = cache.belongCache(url);
			if(index>-1) {

				cacheItem = cache.waitForReady(index);


			}else if(cache.canDel()) {
				synchronized(indexCacheMap){
					System.out.println("stop delete cache");
					cache.stopAllDownload();
					System.out.println("remove from cache:"+key);
					it.remove();
					//indexCacheMap.remove(key);
				}

			}


		}

		return cacheItem;

	}

	public static void buildIndexFrom(String index,ArrayList<String> tsUrls) {

		synchronized (indexCacheMap) {

			index= index.split(".m3u8")[0];
			if(indexCacheMap.get(index)==null)
				indexCacheMap.put(index, new IndexCache(tsUrls));
		}




	}



}
