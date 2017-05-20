package com.weirddev.testme.intellij.template.context;

import com.intellij.openapi.diagnostic.Logger;
import com.weirddev.testme.intellij.utils.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 24/02/2017
 *
 * @author Yaron Yamin
 */
public class GroovyTestBuilderImpl extends JavaTestBuilderImpl {
    private static final Logger LOG = Logger.getInstance(GroovyTestBuilderImpl.class.getName());
    public static final String PARAMS_SEPERATOR = ", ";
    private boolean shouldIgnoreUnusedProperties;
    private final TestBuilder.ParamUsageMode paramUsageMode;
    private final int minPercentOfExcessiveSettersToPreferDefaultCtor;

    public GroovyTestBuilderImpl(int maxRecursionDepth, boolean shouldIgnoreUnusedProperties) {
        super(maxRecursionDepth);
        this.shouldIgnoreUnusedProperties = shouldIgnoreUnusedProperties;
        minPercentOfExcessiveSettersToPreferDefaultCtor = 50;
        paramUsageMode = null;
    }

    public GroovyTestBuilderImpl(int maxRecursionDepth, Method method, boolean shouldIgnoreUnusedProperties, TestBuilder.ParamUsageMode paramUsageMode, int minPercentOfExcessiveSettersToPreferDefaultCtor) {
        super(maxRecursionDepth, method);
        this.shouldIgnoreUnusedProperties = shouldIgnoreUnusedProperties;
        this.paramUsageMode = paramUsageMode;
        this.minPercentOfExcessiveSettersToPreferDefaultCtor = minPercentOfExcessiveSettersToPreferDefaultCtor;
    }

    @Override
    protected void buildCallParam(Map<String, String> replacementTypes, Map<String, String> defaultTypeValues, StringBuilder testBuilder, Node<Param> paramNode) {
        final Type type = paramNode.getData().getType();
        if (isPropertyParam(paramNode)) {
            testBuilder.append(paramNode.getData().getName()).append(" : ");
        }
        if (type.isArray()) {
            testBuilder.append("[");
        }
        buildJavaParam(replacementTypes, defaultTypeValues, testBuilder, paramNode);
        if (type.isArray()) {
            testBuilder.append("] as ").append(type.getCanonicalName()).append("[]");
        }
    }

    private boolean isPropertyParam(Node<Param> paramNode) {
        return paramNode.getData() instanceof SyntheticParam && ((SyntheticParam) paramNode.getData()).isProperty;
    }

    @Override
    protected void buildCallParams(List<? extends Param> params, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues, StringBuilder testBuilder, Node<Param> ownerParamNode) {
        final Type parentContainerClass = ownerParamNode.getData()!=null?ownerParamNode.getData().getType().getParentContainerClass():null;
        final boolean isNonStaticNestedClass = parentContainerClass != null && !ownerParamNode.getData().getType().isStatic();
        if (params != null && params.size()>0 || isNonStaticNestedClass) {
            if (isNonStaticNestedClass) {
                final Node<Param> parentContainerNode = new Node<Param>(new SyntheticParam(parentContainerClass, parentContainerClass.getName(), false), null, ownerParamNode.getDepth());
                buildCallParam(replacementTypes, defaultTypeValues, testBuilder,parentContainerNode);
                testBuilder.append(",");
            }
            buildGroovyCallParams(params, replacementTypes, defaultTypeValues, testBuilder, ownerParamNode);
        } else if(ownerParamNode.getData()!=null){
            List<SyntheticParam> syntheticParams = findProperties(ownerParamNode.getData().getType());
            if (syntheticParams.size() > 0) {
                buildCallParams(syntheticParams, replacementTypes, defaultTypeValues, testBuilder, ownerParamNode);
            }
        }
    }
    protected void buildGroovyCallParams(List<? extends Param> params, Map<String, String> replacementTypes, Map<String, String> defaultTypeValues, StringBuilder testBuilder, Node<Param> ownerParamNode) {
        final int origLength = testBuilder.length();
        if (params != null) {
            for (int i = 0; i < params.size(); i++) {
                final Node<Param> paramNode = new Node<Param>(params.get(i), ownerParamNode, ownerParamNode.getDepth() + 1);
                if (isPropertyParam(paramNode) && shouldIgnoreUnusedProperties && testedMethod!=null && !isPropertyUsed(testedMethod, ownerParamNode.getData().getType(), paramNode.getData())) {
                    continue;
                }
                buildCallParam(replacementTypes, defaultTypeValues, testBuilder, paramNode);
                testBuilder.append(PARAMS_SEPERATOR);
            }
            if (origLength < testBuilder.length()) {
                testBuilder.delete(testBuilder.length() - PARAMS_SEPERATOR.length(),testBuilder.length());
            }
        }
    }

    private boolean isPropertyUsed(@NotNull Method testedMethod, Type paramOwnerType, Param propertyParam) {
        //todo migrate the logic of identifying setters/getters calls to JavaTestBuilder ( direct references)
        if (isReferencedInMethod(testedMethod, paramOwnerType, propertyParam)) return true;
        for (Method calledMethod : testedMethod.getCalledMethods()) {
            if (paramOwnerType.getCanonicalName().equals(calledMethod.getOwnerClassCanonicalType()) && (paramUsageMode== TestBuilder.ParamUsageMode.ReadFrom && calledMethod.isGetter()&& calledMethod.getReturnType().equals(propertyParam.getType()) ||
                    paramUsageMode== TestBuilder.ParamUsageMode.CreateAs && calledMethod.isSetter() && calledMethod.getMethodParams().size()==1 && calledMethod.getMethodParams().get(0).getType().equals(propertyParam.getType()) ) && propertyParam.getName().equals(calledMethod.getPropertyName()) ) {
                return true;
            }
        }
        for (Method method : testedMethod.getCalledFamilyMembers()) {
            if (isReferencedInMethod(method, paramOwnerType, propertyParam)) return true;
        }
        //todo handle cases where property is written implicitly in groovy
        return false;
    }

    private boolean isReferencedInMethod(@NotNull Method testedMethod, Type paramOwnerType, Param propertyParam) {
        for (Reference internalReference : testedMethod.getInternalReferences()) {
            if (paramOwnerType.equals(internalReference.getOwnerType()) && propertyParam.getType().equals(internalReference.getReferenceType()) && propertyParam.getName().equals(internalReference.getReferenceName())) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private List<SyntheticParam> findProperties(Type type) {
        final List<Method> methods = type.getMethods();
        Map<String,SyntheticParam> syntheticParams=new LinkedHashMap<String,SyntheticParam>();
        for (Method method : methods) {
            if (method.isSetter()&&  method.getMethodParams().size()>0 &&method.getPropertyName()!=null) {
                final SyntheticParam syntheticParam = syntheticParams.get(method.getPropertyName());
                if (syntheticParam == null || !propertyMatchesField(type,syntheticParam)) {
                    syntheticParams.put(method.getPropertyName(),new SyntheticParam(method.getMethodParams().get(0).getType(), method.getPropertyName(),true));
                }
            }
        }
        return new ArrayList<SyntheticParam>(syntheticParams.values());
    }

    private boolean propertyMatchesField(Type type, SyntheticParam syntheticParam) {
        Field field = findFieldByName(type, syntheticParam.getName());
        return field != null && field.getType().getCanonicalName().equals(syntheticParam.getType().getCanonicalName());
    }

    @Nullable
    private Field findFieldByName(Type type, String propertyName) {
        for (Field field : type.getFields()) {
            if (propertyName.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    @Override
    protected String resolveNestedClassTypeName(String typeName) {
        return typeName;
    }

    @Nullable
    @Override
    protected Method findValidConstructor(Type type, Map<String, String> replacementTypes, boolean hasEmptyConstructor) {
        final Method constructor = super.findValidConstructor(type, replacementTypes, hasEmptyConstructor);
        if (constructor == null) {
            return null;
        } else if (hasEmptyConstructor) {
            int noOfCtorArgs = constructor.getMethodParams().size();
            int noOfSetters = countSetters(type);
            if (shouldPreferSettersOverCtor(noOfCtorArgs, noOfSetters)) {
                return null; //Prefer default ctor and use setters instead
            } else {
                return constructor;
            }
        } else {
            return constructor;
        }
    }

    boolean shouldPreferSettersOverCtor(int noOfCtorArgs, int noOfSetters) {
        return 0 < noOfSetters && (noOfCtorArgs * ((minPercentOfExcessiveSettersToPreferDefaultCtor + 100f) / 100f) <= ((float)noOfSetters) );
    }

    private int countSetters(Type type) {
        int noOfSetters = 0;
        for (Method method : type.getMethods()) {
            if (method.isSetter()) {
                noOfSetters++;
            }
        }
        return noOfSetters;
    }
}
