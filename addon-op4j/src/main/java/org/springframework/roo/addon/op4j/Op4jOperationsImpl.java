package org.springframework.roo.addon.op4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 *
 * @author Stefan Schmidt
 * @since 1.1
 */
@Component
@Service
public class Op4jOperationsImpl implements Op4jOperations{

	@Reference private MetadataService metadataService;
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	@Reference private ProjectOperations projectOperations;

	public boolean isOp4jAvailable() {
		return getPathResolver() != null;
	}

	public void annotateType(JavaType javaType) {
		Assert.notNull(javaType, "Java type required");

		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata ptm = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(ptm, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		PhysicalTypeDetails ptd = ptm.getPhysicalTypeDetails();
		Assert.notNull(ptd, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, ptd, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) ptd;

		if (null == MemberFindingUtils.getAnnotationOfType(mutableTypeDetails.getTypeAnnotations(), new JavaType(RooOp4j.class.getName()))) {
			JavaType rooSolrSearchable = new JavaType(RooOp4j.class.getName());
			if (!mutableTypeDetails.getTypeAnnotations().contains(rooSolrSearchable)) {
				mutableTypeDetails.addTypeAnnotation(new DefaultAnnotationMetadata(rooSolrSearchable, new ArrayList<AnnotationAttributeValue<?>>()));
			}
		}
	}
	
	public void setup() {
		Element configuration = XmlUtils.getConfiguration(getClass());

		List<Element> dependencies = XmlUtils.findElements("/configuration/op4j/dependencies/dependency", configuration);
		for (Element dependency : dependencies) {
			projectOperations.dependencyUpdate(new Dependency(dependency));
		}
	}
	
	/**
	 * @return the path resolver or null if there is no user project
	 */
	private PathResolver getPathResolver() {
		ProjectMetadata projectMetadata = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (projectMetadata == null) {
			return null;
		}
		return projectMetadata.getPathResolver();
	}
}