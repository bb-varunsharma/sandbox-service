{{/* vim: set filetype=mustache: */}}

{{/*
Renders nodeSelector block in the manifests
*/}}

{{- define "app.uniqueSuffix" }}
{{- $uniqueSuffix := "" -}}
{{/*
{{- if .tenant }}
{{- printf "-%s" .tenant -}}
{{- end -}}
*/}}
{{- if .project }}
{{- printf "-%s" .project }}
{{- end -}}
{{- end -}}


{{- define "sandbox.nodeLabels" }}
{{- if . }}
{{ toYaml . }}
{{- end -}}
{{- end -}}

{{- define "sandbox.nodeAffinity" }}
{{- $nodeAffinityLabels := . -}}
nodeAffinity:
  requiredDuringSchedulingIgnoredDuringExecution:
    nodeSelectorTerms:
      - matchExpressions:
          {{- range $index, $label := $nodeAffinityLabels }}
          - key: {{ $label.name }}
            operator: In
            values:
              - {{ $label.value }}
          {{- end }}
{{- end -}}

{{- define "sandbox.requiredPodAntiAffinity" }}
{{- $podAntiAffinityLabels := . -}}
requiredDuringSchedulingIgnoredDuringExecution:
  - labelSelector:
      matchExpressions:
        {{- range $index, $label := $podAntiAffinityLabels }}
        - key: {{ $label.name }}
          operator: In
          values:
            - {{ $label.value }}
        {{- end }}
    topologyKey: kubernetes.io/hostname
{{- end -}}

{{- define "sandbox.preferredPodAntiAffinity" }}
{{- $podAntiAffinityLabels := . -}}
preferredDuringSchedulingIgnoredDuringExecution:
  - weight: 100
    podAffinityTerm:
      labelSelector:
        matchExpressions:
          {{- range $index, $label := $podAntiAffinityLabels }}
          - key: {{ $label.name }}
            operator: In
            values:
              - {{ $label.value }}
          {{- end }}
      topologyKey: kubernetes.io/hostname
{{- end -}}

{{/*
Renders affinity block in the manifests
*/}}
{{- define "sandbox.affinity" }}
affinity:
  {{- if .nodeAffinityLabels }}
  {{- include "sandbox.nodeAffinity" .nodeAffinityLabels | nindent 2 }}
  {{- end }}
  {{- if .podAntiAffinity }}
  podAntiAffinity:
  {{- if eq .podAntiAffinity.type "required" }}
  {{- include "sandbox.requiredPodAntiAffinity" .podAntiAffinity.labels | nindent 4 }}
  {{- end }}
  {{- if eq .podAntiAffinity.type "preferred" }}
  {{- include "sandbox.preferredPodAntiAffinity" .podAntiAffinity.labels | nindent 4 }}
  {{- end }}
  {{- end }}
{{- end -}}

{{- define "sandbox.configmap" }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .config.name }}{{ include "app.uniqueSuffix" .values }}
  namespace: {{ .namespace }}
data:
{{- toYaml .config.data | nindent 2 }}
{{- end }}

{{- define "sandbox.configFile" }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .config.name }}{{ include "app.uniqueSuffix" .values }}
  namespace: {{ .namespace }}
data:
  {{- (.root.Files.Glob (printf "%s" .config.file)).AsConfig | nindent 2 }}
{{- end }}

{{/*
Renders secret based on the secrets.yaml
*/}}
{{- define "sandbox.secretsMap" -}}
apiVersion: v1
kind: Secret
type: Opaque
metadata:
  name: {{ .secret.name }}{{ include "app.uniqueSuffix" .values }}
  namespace: {{ .namespace }}
data:
{{- toYaml .secret.data | nindent 2 }}
{{- end }}

{{/*
Renders secret based on the secrets.yaml
*/}}
{{- define "sandbox.secretsFile" -}}
apiVersion: v1
kind: Secret
type: Opaque
metadata:
  name: {{ .secret.name }}{{ include "app.uniqueSuffix" .values }}
  namespace: {{ .namespace }}
data:
  {{- (.root.Files.Glob (printf "%s" .secret.file)).AsSecrets | nindent 2 }}
{{- end }}

{{/*
Renders VolumeMounts for the Container in a Pod
*/}}
{{- define "sandbox.containerVolumeMounts" -}}
{{ $values := .values }}
{{- range $index, $volumeMountData := .configFiles -}}
{{- if and ($volumeMountData.file) ($volumeMountData.mountPath) }}
- mountPath: {{ $volumeMountData.mountPath }}
  subPath: {{ $volumeMountData.file }}
  name: {{ $volumeMountData.name }}{{ include "app.uniqueSuffix" $values }}
  readOnly: true
{{- end }}
{{- end }}
{{- end -}}

{{/*
Renders VolumeMounts for the Pod
*/}}
{{- define "sandbox.podVolumes" -}}
{{- $values := .values -}}
{{- if eq .type "secretsFiles" }}
{{- range .secretsFiles -}}
- secret:
    secretName: {{ .name }}{{ include "app.uniqueSuffix" $values }}
  name: {{ .name }}{{ include "app.uniqueSuffix" $values }}
{{ end }}
{{- else -}}
{{- range .configFiles -}}
- configMap:
    name: {{ .name }}{{ include "app.uniqueSuffix" $values }}
  name: {{ .name }}{{ include "app.uniqueSuffix" $values }}
{{ end }}
{{- end -}}
{{- end -}}

{{/*
Renders Pod Annotations. Handy to roll the Pod as and when a configmap/secret changes.
*/}}
{{- define "sandbox.infraAnnotations" -}}
{{- $root := . -}}
{{- $namespace := .Values.namespace -}}
checksum/infra-secrets: {{ include (print $.Template.BasePath "/infra-secrets.yaml") . | sha256sum }}
checksum/configmap: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
checksum/secrets: {{ include (print $.Template.BasePath "/secreds.yaml") . | sha256sum }}
{{- end }}

{{/*
Renders pod Annotations.
*/}}
{{- define "sandbox.podAnnotations" -}}
{{- if hasKey . "podAnnotations" }}
{{- toYaml .podAnnotations }}
{{- end }}
{{- end }}

{{- define "sandbox.nativeHPA"}}
apiVersion: autoscaling/v2beta2
kind: HorizontalPodAutoscaler
metadata:
  labels:
    app: {{ .deploymentName }}
    env: {{ .environment }}
  name: {{ .deploymentName }}{{ include "app.uniqueSuffix" .values }}
  namespace: {{ .namespace }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ .config.targetDeployment }}{{ include "app.uniqueSuffix" .values }}
  minReplicas: {{ .config.minReplicas }}
  maxReplicas: {{ .config.maxReplicas }}
  metrics:
  {{- if hasKey .config "targetCPUAverage" }}
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: {{ .config.targetCPUAverage }}
  {{- end }}
  {{- if hasKey .config "targetMemoryAverage" }}
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: {{ .config.targetMemoryAverage }}
  {{- end }}
{{- end }}

{{- define "sandbox.imagePullSecrets" }}
{{- $prodImagePullSecretName := "acr-login" }}
imagePullSecrets:
- name: {{ $prodImagePullSecretName }}
{{- end -}}

{{/*
Generate ScaledObject name
*/}}
{{- define "sandbox.scaledObjectName" -}}
{{- printf "%s-%s" .Release.Name "scaledobject" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

