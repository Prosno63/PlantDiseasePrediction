package bd.fasol.knowledge.dto.response; import bd.fasol.model.Disease;
public record DiseaseResponse(Long id,Long cropId,String nameBn,String nameEn,String modelClassLabel,boolean active){public static DiseaseResponse from(Disease x){return new DiseaseResponse(x.id,x.crop.id,x.nameBn,x.nameEn,x.modelClassLabel,x.isActive);}}
