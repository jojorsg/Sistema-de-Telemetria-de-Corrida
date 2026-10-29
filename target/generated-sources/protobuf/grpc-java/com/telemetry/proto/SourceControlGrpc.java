package com.telemetry.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.59.0)",
    comments = "Source: telemetry.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class SourceControlGrpc {

  private SourceControlGrpc() {}

  public static final java.lang.String SERVICE_NAME = "telemetry.SourceControl";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.telemetry.proto.ControlCommand,
      com.telemetry.proto.ControlResponse> getSendCommandMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendCommand",
      requestType = com.telemetry.proto.ControlCommand.class,
      responseType = com.telemetry.proto.ControlResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.telemetry.proto.ControlCommand,
      com.telemetry.proto.ControlResponse> getSendCommandMethod() {
    io.grpc.MethodDescriptor<com.telemetry.proto.ControlCommand, com.telemetry.proto.ControlResponse> getSendCommandMethod;
    if ((getSendCommandMethod = SourceControlGrpc.getSendCommandMethod) == null) {
      synchronized (SourceControlGrpc.class) {
        if ((getSendCommandMethod = SourceControlGrpc.getSendCommandMethod) == null) {
          SourceControlGrpc.getSendCommandMethod = getSendCommandMethod =
              io.grpc.MethodDescriptor.<com.telemetry.proto.ControlCommand, com.telemetry.proto.ControlResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendCommand"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.telemetry.proto.ControlCommand.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.telemetry.proto.ControlResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SourceControlMethodDescriptorSupplier("SendCommand"))
              .build();
        }
      }
    }
    return getSendCommandMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.telemetry.proto.StateRequest,
      com.telemetry.proto.StateResponse> getGetStateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetState",
      requestType = com.telemetry.proto.StateRequest.class,
      responseType = com.telemetry.proto.StateResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.telemetry.proto.StateRequest,
      com.telemetry.proto.StateResponse> getGetStateMethod() {
    io.grpc.MethodDescriptor<com.telemetry.proto.StateRequest, com.telemetry.proto.StateResponse> getGetStateMethod;
    if ((getGetStateMethod = SourceControlGrpc.getGetStateMethod) == null) {
      synchronized (SourceControlGrpc.class) {
        if ((getGetStateMethod = SourceControlGrpc.getGetStateMethod) == null) {
          SourceControlGrpc.getGetStateMethod = getGetStateMethod =
              io.grpc.MethodDescriptor.<com.telemetry.proto.StateRequest, com.telemetry.proto.StateResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetState"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.telemetry.proto.StateRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.telemetry.proto.StateResponse.getDefaultInstance()))
              .setSchemaDescriptor(new SourceControlMethodDescriptorSupplier("GetState"))
              .build();
        }
      }
    }
    return getGetStateMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static SourceControlStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SourceControlStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SourceControlStub>() {
        @java.lang.Override
        public SourceControlStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SourceControlStub(channel, callOptions);
        }
      };
    return SourceControlStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static SourceControlBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SourceControlBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SourceControlBlockingStub>() {
        @java.lang.Override
        public SourceControlBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SourceControlBlockingStub(channel, callOptions);
        }
      };
    return SourceControlBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static SourceControlFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<SourceControlFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<SourceControlFutureStub>() {
        @java.lang.Override
        public SourceControlFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new SourceControlFutureStub(channel, callOptions);
        }
      };
    return SourceControlFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void sendCommand(com.telemetry.proto.ControlCommand request,
        io.grpc.stub.StreamObserver<com.telemetry.proto.ControlResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendCommandMethod(), responseObserver);
    }

    /**
     */
    default void getState(com.telemetry.proto.StateRequest request,
        io.grpc.stub.StreamObserver<com.telemetry.proto.StateResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetStateMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service SourceControl.
   */
  public static abstract class SourceControlImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return SourceControlGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service SourceControl.
   */
  public static final class SourceControlStub
      extends io.grpc.stub.AbstractAsyncStub<SourceControlStub> {
    private SourceControlStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SourceControlStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SourceControlStub(channel, callOptions);
    }

    /**
     */
    public void sendCommand(com.telemetry.proto.ControlCommand request,
        io.grpc.stub.StreamObserver<com.telemetry.proto.ControlResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendCommandMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getState(com.telemetry.proto.StateRequest request,
        io.grpc.stub.StreamObserver<com.telemetry.proto.StateResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service SourceControl.
   */
  public static final class SourceControlBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<SourceControlBlockingStub> {
    private SourceControlBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SourceControlBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SourceControlBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.telemetry.proto.ControlResponse sendCommand(com.telemetry.proto.ControlCommand request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendCommandMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.telemetry.proto.StateResponse getState(com.telemetry.proto.StateRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetStateMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service SourceControl.
   */
  public static final class SourceControlFutureStub
      extends io.grpc.stub.AbstractFutureStub<SourceControlFutureStub> {
    private SourceControlFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected SourceControlFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new SourceControlFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.telemetry.proto.ControlResponse> sendCommand(
        com.telemetry.proto.ControlCommand request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendCommandMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.telemetry.proto.StateResponse> getState(
        com.telemetry.proto.StateRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetStateMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_COMMAND = 0;
  private static final int METHODID_GET_STATE = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_COMMAND:
          serviceImpl.sendCommand((com.telemetry.proto.ControlCommand) request,
              (io.grpc.stub.StreamObserver<com.telemetry.proto.ControlResponse>) responseObserver);
          break;
        case METHODID_GET_STATE:
          serviceImpl.getState((com.telemetry.proto.StateRequest) request,
              (io.grpc.stub.StreamObserver<com.telemetry.proto.StateResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendCommandMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.telemetry.proto.ControlCommand,
              com.telemetry.proto.ControlResponse>(
                service, METHODID_SEND_COMMAND)))
        .addMethod(
          getGetStateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.telemetry.proto.StateRequest,
              com.telemetry.proto.StateResponse>(
                service, METHODID_GET_STATE)))
        .build();
  }

  private static abstract class SourceControlBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    SourceControlBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.telemetry.proto.TelemetryProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("SourceControl");
    }
  }

  private static final class SourceControlFileDescriptorSupplier
      extends SourceControlBaseDescriptorSupplier {
    SourceControlFileDescriptorSupplier() {}
  }

  private static final class SourceControlMethodDescriptorSupplier
      extends SourceControlBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    SourceControlMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (SourceControlGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new SourceControlFileDescriptorSupplier())
              .addMethod(getSendCommandMethod())
              .addMethod(getGetStateMethod())
              .build();
        }
      }
    }
    return result;
  }
}
