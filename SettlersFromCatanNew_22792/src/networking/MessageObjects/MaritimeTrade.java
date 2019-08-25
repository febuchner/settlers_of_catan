package networking.MessageObjects;

import com.google.gson.annotations.SerializedName;

public class MaritimeTrade {

	@SerializedName("Angebot") private Resources resourcesSupply;
	@SerializedName("Nachfrage") private Resources resourcesDemand;
	
	public MaritimeTrade(Resources resourcesSupply, Resources resourcesDemand) {
		this.resourcesSupply = resourcesSupply;
		this.resourcesDemand = resourcesDemand;
	}

	public Resources getResourcesSupply() {
		return resourcesSupply;
	}

	public void setResourcesSupply(Resources resourcesSupply) {
		this.resourcesSupply = resourcesSupply;
	}

	public Resources getResourcesDemand() {
		return resourcesDemand;
	}

	public void setResourcesDemand(Resources resourcesDemand) {
		this.resourcesDemand = resourcesDemand;
	}
	
}
