#!/usr/bin/env python3
"""
Fonte B - Sensor Ajustável (Controlável) - Implementação em Python.
- Listener multicast para descoberta do Gateway
- Servidor gRPC para comandos de controle
- Envio de dados por UDP
"""
import socket, struct, threading, time, random, sys
import grpc
from concurrent import futures
import telemetry_pb2
import telemetry_pb2_grpc

MULTICAST_GROUP = "230.0.0.1"
MULTICAST_PORT  = 5002
SOURCE_ID       = "SENSOR-AJUSTAVEL-01"
DOMAIN_TYPE     = "sensor_ajustavel"
GRPC_PORT       = 5102

state = {
    "enabled": True, "frequency": 5, "threshold": 50.0,
    "gateway_ip": None, "gateway_udp_port": None,
}

def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except Exception:
        return "127.0.0.1"
    finally:
        s.close()

LOCAL_IP = get_local_ip()


class SourceControlServicer(telemetry_pb2_grpc.SourceControlServicer):
    def SendCommand(self, request, context):
        cmd, value = request.command_type, request.value
        try:
            if   cmd == "ENABLE":        state["enabled"] = True;  msg = "Fonte ativada"
            elif cmd == "DISABLE":       state["enabled"] = False; msg = "Fonte desativada"
            elif cmd == "SET_FREQUENCY": state["frequency"] = int(value); msg = f"Freq {value}s"
            elif cmd == "SET_THRESHOLD": state["threshold"] = float(value); msg = f"Limiar {value}"
            else:
                return telemetry_pb2.ControlResponse(success=False, message=f"Não suportado: {cmd}")
            cur = f"enabled={state['enabled']};freq={state['frequency']};threshold={state['threshold']}"
            return telemetry_pb2.ControlResponse(success=True, message=msg, current_state=cur)
        except Exception as e:
            return telemetry_pb2.ControlResponse(success=False, message=f"Erro: {e}")

    def GetState(self, request, context):
        return telemetry_pb2.StateResponse(
            source_id=SOURCE_ID, enabled=state["enabled"],
            send_frequency_seconds=state["frequency"], threshold=state["threshold"])


def multicast_listener():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(("", MULTICAST_PORT))
    mreq = struct.pack("4sl", socket.inet_aton(MULTICAST_GROUP), socket.INADDR_ANY)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
    print(f"[{SOURCE_ID}] Escutando multicast em {MULTICAST_GROUP}:{MULTICAST_PORT}")
    while True:
        data, _ = sock.recvfrom(8192)
        try:
            req = telemetry_pb2.DiscoveryRequest()
            req.ParseFromString(data)
            if req.message == "TELEMETRY_DISCOVERY":
                state["gateway_ip"] = req.gateway_ip
                state["gateway_udp_port"] = req.gateway_udp_port
                print(f"[{SOURCE_ID}] Gateway: {req.gateway_ip}:{req.gateway_udp_port}")
                resp = telemetry_pb2.DiscoveryResponse(
                    source_id=SOURCE_ID, source_type="controllable",
                    domain_type=DOMAIN_TYPE, ip=LOCAL_IP, udp_port=0,
                    grpc_port=GRPC_PORT,
                    initial_state=f"enabled={state['enabled']};freq={state['frequency']}")
                sock.sendto(resp.SerializeToString(), (MULTICAST_GROUP, MULTICAST_PORT))
        except Exception as e:
            print(f"[{SOURCE_ID}] Erro multicast: {e}")


def data_producer():
    while True:
        try:
            if state["enabled"] and state["gateway_ip"]:
                value = 30 + random.random() * 40
                is_event = value > state["threshold"]
                data = telemetry_pb2.TelemetryData(
                    source_id=SOURCE_ID, source_type=DOMAIN_TYPE,
                    timestamp=int(time.time() * 1000),
                    is_event=is_event,
                    event_description="Vibração acima do limiar" if is_event else "")
                data.metrics["vibracao"] = value
                data.labels["threshold"] = str(state["threshold"])

                udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                udp.sendto(data.SerializeToString(),
                           (state["gateway_ip"], state["gateway_udp_port"]))
                udp.close()
                tag = " [EVENTO]" if is_event else ""
                print(f"[{SOURCE_ID}] Enviado vibracao={value:.2f}{tag}")
            time.sleep(state["frequency"])
        except Exception as e:
            print(f"[{SOURCE_ID}] Erro producer: {e}")
            time.sleep(2)


def serve_grpc():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=5))
    telemetry_pb2_grpc.add_SourceControlServicer_to_server(SourceControlServicer(), server)
    server.add_insecure_port(f"[::]:{GRPC_PORT}")
    server.start()
    print(f"[{SOURCE_ID}] gRPC escutando na porta {GRPC_PORT}")
    server.wait_for_termination()


if __name__ == "__main__":
    print(f"[{SOURCE_ID}] Iniciando. IP local: {LOCAL_IP}")
    threading.Thread(target=multicast_listener, daemon=True).start()
    threading.Thread(target=data_producer,     daemon=True).start()
    serve_grpc()