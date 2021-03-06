package com.dotmarketing.portlets.contentlet.util;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.FileUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContentletUtil {
	
	//http://jira.dotmarketing.net/browse/DOTCMS-3393
	public static Map<String, Object> getHostFolderInfo(String hostFolderId, User user) throws DotDataException, DotSecurityException {
		String hostName = "";
		String path = "";
		String hostFolderPath ="";
		Map<String, Object> hostOrFolderMap = new HashMap<String, Object>();
		Host systemHost = APILocator.getHostAPI().findSystemHost(APILocator.getUserAPI().getSystemUser(), false);
				
		if(!UtilMethods.isSet(hostFolderId) || hostFolderId.equals("allHosts")){
			hostFolderId="";
			hostFolderPath = "";
			hostOrFolderMap.put("host", "");
			hostOrFolderMap.put("folder", "");
			hostOrFolderMap.put("path", "");
		}
		if(hostFolderId.equalsIgnoreCase(systemHost.getIdentifier()) ||
				hostFolderId.equalsIgnoreCase(FolderAPI.SYSTEM_FOLDER)){
			hostFolderPath = "all";
			hostOrFolderMap.put("host", "");
			hostOrFolderMap.put("folder", "");
			hostOrFolderMap.put("path", hostFolderPath);
			return hostOrFolderMap;
		}
		if(InodeUtils.isSet(hostFolderId)){
			Host host = APILocator.getHostAPI().find(hostFolderId, user, false);
			if(host != null) {
				hostName = host.getHostname();	
				hostOrFolderMap.put("path", hostName);
				hostOrFolderMap.put("host", host.getIdentifier());
				hostOrFolderMap.put("folder", "");
			} else {
				Folder folder = APILocator.getFolderAPI().find(hostFolderId,user,false);
				path = APILocator.getIdentifierAPI().find(folder).getPath();
				host = APILocator.getHostAPI().find(folder.getHostId(), user, false);
				hostName = host.getHostname();
				path.substring(path.length()-1);
				hostFolderPath = hostName + path;
				hostOrFolderMap.put("path",hostFolderPath);
				hostOrFolderMap.put("host", host.getIdentifier());
				hostOrFolderMap.put("folder", folder.getInode());
			}
		}	

		return hostOrFolderMap;
	}
	
	public static String sanitizeFileName(String fileName){
		return FileUtil.sanitizeFileName(fileName);
	}

	/**
	 * This method will retrieve the values of a special field, for example Binary, Categories and Tags.
	 * That information will be placed under the same Contentlet.
	 *
	 * @param user User from Front End with permission to read Special Fields.
	 * @param contentlet Contentlet that needs the special fields set.
	 *
	 * @return Contentlet with the values in place.
	 *
	 * @throws DotDataException
     */
	public static Contentlet setSpecialFieldValues(User user, Contentlet contentlet) throws DotDataException{
		Map<String, Object> m = new HashMap<>();
		Structure s = contentlet.getStructure();

		for(Field f : FieldsCache.getFieldsByStructureInode(s.getInode())){
			if(f.getFieldType().equals(Field.FieldType.BINARY.toString())){
				m.put(f.getVelocityVarName(), "/contentAsset/raw-data/" +  contentlet.getIdentifier() + "/" + f.getVelocityVarName());
				m.put(f.getVelocityVarName() + "ContentAsset", contentlet.getIdentifier() + "/" +f.getVelocityVarName());

			} else if(f.getFieldType().equals(Field.FieldType.CATEGORY.toString())) {
				List<Category> cats = null;

				try {
					cats = APILocator.getCategoryAPI().getParents(contentlet, user, true);
				} catch (Exception e) {
					Logger.error(ContentletUtil.class,
							String.format("Unable to get the Categories for given contentlet with inode= %s", contentlet.getInode()));
				}

				if(cats!=null && !cats.isEmpty()) {
					String catsStr = cats.stream().map(Category::getCategoryName).collect(Collectors.joining(", "));
					m.put(f.getVelocityVarName(), catsStr);
				}
			}
		}

		contentlet.getMap().putAll(m);
		contentlet.setTags();

		return contentlet;
	}
	
}


