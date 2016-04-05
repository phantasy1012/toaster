package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.toaster.impl.ToasterProvider;

public class ToasterModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321.AbstractToasterModule {
    public ToasterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ToasterModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321.ToasterModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
    	final ToasterProvider provider = new ToasterProvider();
    	
    	DataBroker dataBrokerService = getDataBrokerDependency();
    	provider.setDataProvider(dataBrokerService);
    	
    	getBrokerDependency().registerProvider(provider);
//    	return provider;
    	// Wrap toaster as AutoCloseable and close registrations to md-sal at
        // close(). The close method is where you would generally clean up thread pools
        // etc.
        final class AutoCloseableToaster implements AutoCloseable {

            @Override
            public void close() throws Exception {
            	provider.close();
            }
        }
        return new AutoCloseableToaster();
    }

}
