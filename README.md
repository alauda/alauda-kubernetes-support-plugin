## Alauda Kubernetes Support Plugin


Example controller implementation:
```java

@Extension
public class PipelineConfigController implements Controller<V1alpha1PipelineConfig, V1alpha1PipelineConfigList> {
    private static final Logger logger = Logger.getLogger(PipelineConfigController.class.getName());

    private SharedIndexInformer<V1alpha1PipelineConfig> pipelineConfigInformer;

    @Override
    public void initialize(ApiClient client, SharedInformerFactory factory) {
        DevopsAlaudaIoV1alpha1Api api = new DevopsAlaudaIoV1alpha1Api();
        pipelineConfigInformer = factory.sharedIndexInformerFor(callGeneratorParams -> {
            try {
                return api.listPipelineConfigForAllNamespacesCall(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        callGeneratorParams.resourceVersion,
                        callGeneratorParams.timeoutSeconds,
                        callGeneratorParams.watch,
                        null,
                        null
                );
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        }, V1alpha1PipelineConfig.class, V1alpha1PipelineConfigList.class);

        pipelineConfigInformer.addEventHandler(new ResourceEventHandler<V1alpha1PipelineConfig>() {
            @Override
            public void onAdd(V1alpha1PipelineConfig obj) {
                logger.warning(String.format("Add %s", obj.getMetadata().getName()));
            }

            @Override
            public void onUpdate(V1alpha1PipelineConfig oldObj, V1alpha1PipelineConfig newObj) {
                logger.warning(String.format("Update %s", newObj.getMetadata().getName()));
            }

            @Override
            public void onDelete(V1alpha1PipelineConfig obj, boolean deletedFinalStateUnknown) {

            }
        });
    }

    @Override
    public void start() {
        logger.warning("Start controller for pipeline config");
        logger.warning(String.format("Index has %d pipelineconfigs", pipelineConfigInformer.getIndexer().list().size()));
    }

    @Override
    public void shutDown(Throwable reason) {

    }

    @Override
    public boolean hasSynced() {
        return pipelineConfigInformer.hasSynced();
    }

    @Override
    public Type getType() {
        return new TypeToken<V1alpha1PipelineConfig>(){}.getType();
    }
```