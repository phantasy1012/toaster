/*
 * Copyright © 2015 SKSPruce, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.toaster.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321.Toaster;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321.modules.module.configuration.Toaster;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.impl.rev160321.modules.module.configuration.ToasterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev160320.DisplayString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev160320.Toaster;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev160320.Toaster.ToasterStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.toaster.rev160320.ToasterBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ToasterProvider implements BindingAwareProvider, AutoCloseable {

   private static final Logger LOG = LoggerFactory.getLogger(ToasterProvider.class);

   //making this public because this unique ID is required later on in other classes.
   public static final InstanceIdentifier<Toaster>  TOASTER_IID = InstanceIdentifier.builder(Toaster.class).build();
      
   private static final DisplayString TOASTER_MANUFACTURER = new DisplayString("Opendaylight");
   private static final DisplayString TOASTER_MODEL_NUMBER = new DisplayString("Model 1 - Binding Aware");
    
   private DataBroker dataProvider;
  
   private Toaster buildToaster( ToasterStatus status ) {
       
       // note - we are simulating a device whose manufacture and model are
       // fixed (embedded) into the hardware.
       // This is why the manufacture and model number are hardcoded.
       return new ToasterBuilder().setToasterManufacturer( TOASTER_MANUFACTURER )
                                  .setToasterModelNumber( TOASTER_MODEL_NUMBER )
                                  .setToasterStatus( status )
                                  .build();
   }
   
   public void setDataProvider( final DataBroker salDataProvider ) {
        this.dataProvider = salDataProvider;
        setToasterStatusUp( null );
   }
 
    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("ToasterProvider Session Initiated");
    }

   @Override
   public void close() throws Exception {
        LOG.info("ToasterProvider Closed");
        if (dataProvider != null) {
            WriteTransaction t = dataProvider.newWriteOnlyTransaction();
            t.delete(LogicalDatastoreType.OPERATIONAL,TOASTER_IID);
            ListenableFuture<RpcResult<TransactionStatus>> future = t.commit();
            Futures.addCallback( future, new FutureCallback<RpcResult<TransactionStatus>>() {
			@Override
                public void onSuccess( RpcResult<TransactionStatus> result ) {
                    LOG.debug( "Delete Toaster commit result: " + result );
			}

			@Override
                public void onFailure( Throwable t ) {
				LOG.error("Delete of Toaster failed", t);
			}
		});
        }
   }
   
   private void setToasterStatusUp( final Function<Boolean,Void> resultCallback ) {
       
       WriteTransaction tx = dataProvider.newWriteOnlyTransaction();
       tx.put( LogicalDatastoreType.OPERATIONAL,TOASTER_IID, buildToaster( ToasterStatus.Up ) );
       
       ListenableFuture<RpcResult<TransactionStatus>> commitFuture = tx.commit();
       
       Futures.addCallback( commitFuture, new FutureCallback<RpcResult<TransactionStatus>>() {
           @Override
           public void onSuccess( RpcResult<TransactionStatus> result ) {
               if( result.getResult() != TransactionStatus.COMMITED ) {
                   LOG.error( "Failed to update toaster status: " + result.getErrors() );
               }
               
               notifyCallback( result.getResult() == TransactionStatus.COMMITED );
           }
           
           @Override
           public void onFailure( Throwable t ) {
               // We shouldn't get an OptimisticLockFailedException (or any ex) as no
               // other component should be updating the operational state.
               LOG.error( "Failed to update toaster status", t );
               
               notifyCallback( false );
           }
           
           void notifyCallback( boolean result ) {
               if( resultCallback != null ) {
                   resultCallback.apply( result );
               }
           }
       } );
   }

}
